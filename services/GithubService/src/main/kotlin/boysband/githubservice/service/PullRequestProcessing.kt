package boysband.githubservice.service

import boysband.githubservice.cache.EventStateCache
import boysband.githubservice.model.response.PullRequestEventResponse
import boysband.githubservice.model.utils.Comment
import boysband.githubservice.model.utils.Event
import boysband.githubservice.model.resourse.Commit
import boysband.githubservice.model.resourse.PullRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.body

@Service
class PullRequestProcessing(
    private val utilsProcessing: UtilsProcessing,
    private val eventStateCache: EventStateCache
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun processPullRequest(chatId: Long, pullRequest: PullRequest, token: String): PullRequestEventResponse {
        val prDetails = getPullRequestDetails(pullRequest, token) ?: pullRequest
        val comments = getComments(pullRequest, token) ?: emptyList()
        val reviewComments = getReviewComments(pullRequest, token) ?: emptyList()
        val allComments = comments + reviewComments
        val events = getEvents(pullRequest, token) ?: emptyList()
        val commits = getCommits(pullRequest, token) ?: emptyList()

        val cacheKey = eventStateCache.buildKey(chatId, pullRequest.owner, pullRequest.repo, "pr", pullRequest.prNumber.toString())

        val (newComments, updatedComments) = filterNewAndUpdatedComments(cacheKey, allComments)
        val (newEvents, updatedEvents) = filterNewAndUpdatedEvents(cacheKey, events)
        val newCommits = filterNewCommits(cacheKey, commits)

        updateCache(cacheKey, allComments, events, commits)

        return PullRequestEventResponse(
            pullRequest = prDetails,
            newComments = newComments,
            updatedComments = updatedComments,
            newEvents = newEvents,
            updatedEvents = updatedEvents,
            newCommits = newCommits
        )
    }

    fun getPullRequestDetails(pullRequest: PullRequest, token: String): PullRequest? {
        return try {
            utilsProcessing.baseGithubRequestUrl(pullRequest, "", token)
                ?.onStatus({ it.isError }) { _, response ->
                    throw RuntimeException("GitHub API error ${response.statusCode} for PR details")
                }
                ?.body<PullRequest>()
                ?.copy(owner = pullRequest.owner, repo = pullRequest.repo)
        } catch (e: Exception) {
            logger.error("Failed to fetch PR details for ${pullRequest.owner}/${pullRequest.repo}#${pullRequest.prNumber}", e)
            null
        }
    }

    fun getComments(pullRequest: PullRequest, token: String): List<Comment>? {
        // GitHub API: PR conversation comments live under /issues/{number}/comments, not /pulls/{number}/comments
        return try {
            utilsProcessing.baseGithubIssueStyleRequest(
                pullRequest.owner,
                pullRequest.repo,
                pullRequest.prNumber,
                "/comments",
                token
            )
                ?.onStatus({ it.isError }) { _, response ->
                    throw RuntimeException("GitHub API error ${response.statusCode} for PR comments")
                }
                ?.body<List<Comment>>()
        } catch (e: Exception) {
            logger.error("Failed to fetch PR comments for ${pullRequest.owner}/${pullRequest.repo}#${pullRequest.prNumber}", e)
            null
        }
    }

    fun getReviewComments(pullRequest: PullRequest, token: String): List<Comment>? {
        // GitHub API: /pulls/{number}/comments returns inline code review comments
        return try {
            utilsProcessing.baseGithubRequestUrl(pullRequest, "/comments", token)
                ?.onStatus({ it.isError }) { _, response ->
                    throw RuntimeException("GitHub API error ${response.statusCode} for PR review comments")
                }
                ?.body<List<Comment>>()
        } catch (e: Exception) {
            logger.error("Failed to fetch PR review comments for ${pullRequest.owner}/${pullRequest.repo}#${pullRequest.prNumber}", e)
            null
        }
    }

    fun getEvents(pullRequest: PullRequest, token: String): List<Event>? {
        // GitHub API: PR events live under /issues/{number}/events, not /pulls/{number}/events
        return try {
            utilsProcessing.baseGithubIssueStyleRequest(
                pullRequest.owner,
                pullRequest.repo,
                pullRequest.prNumber,
                "/events",
                token
            )
                ?.onStatus({ it.isError }) { _, response ->
                    throw RuntimeException("GitHub API error ${response.statusCode} for PR events")
                }
                ?.body<List<Event>>()
        } catch (e: Exception) {
            logger.error("Failed to fetch PR events for ${pullRequest.owner}/${pullRequest.repo}#${pullRequest.prNumber}", e)
            null
        }
    }

    fun getCommits(pullRequest: PullRequest, token: String): List<Commit>? {
        return try {
            utilsProcessing.baseGithubRequestUrl(pullRequest, "/commits", token)
                ?.onStatus({ it.isError }) { _, response ->
                    throw RuntimeException("GitHub API error ${response.statusCode} for PR commits")
                }
                ?.body<List<Commit>>()
                ?.map { it.copy(owner = pullRequest.owner, repo = pullRequest.repo) }
        } catch (e: Exception) {
            logger.error("Failed to fetch PR commits for ${pullRequest.owner}/${pullRequest.repo}#${pullRequest.prNumber}", e)
            null
        }
    }

    private fun filterNewAndUpdatedComments(
        cacheKey: String,
        comments: List<Comment>
    ): Pair<List<Comment>, List<Comment>> {
        val lastCommentId = eventStateCache.getLastCommentId(cacheKey)

        if (lastCommentId == null) {
            return Pair(emptyList(), emptyList())
        }

        val newComments = mutableListOf<Comment>()
        val updatedComments = mutableListOf<Comment>()

        for (comment in comments) {
            if (comment.id > lastCommentId) {
                newComments.add(comment)
            } else {
                val cachedUpdatedAt = eventStateCache.getCommentUpdatedAt("$cacheKey:comment:${comment.id}")
                if (cachedUpdatedAt != null && cachedUpdatedAt != comment.updatedAt) {
                    updatedComments.add(comment)
                }
            }
        }

        return Pair(newComments, updatedComments)
    }

    private fun filterNewAndUpdatedEvents(cacheKey: String, events: List<Event>): Pair<List<Event>, List<Event>> {
        val lastEventId = eventStateCache.getLastEventId(cacheKey)

        if (lastEventId == null) {
            return Pair(emptyList(), emptyList())
        }

        val newEvents = mutableListOf<Event>()
        val updatedEvents = mutableListOf<Event>()

        for (event in events) {
            if (event.id > lastEventId) {
                newEvents.add(event)
            } else {
                val cachedUpdatedAt = eventStateCache.getEventCreatedAt("$cacheKey:event:${event.id}")
                if (cachedUpdatedAt != null && cachedUpdatedAt != event.updatedAt) {
                    updatedEvents.add(event)
                }
            }
        }

        return Pair(newEvents, updatedEvents)
    }

    private fun filterNewCommits(cacheKey: String, commits: List<Commit>): List<Commit> {
        val lastCommitSha = eventStateCache.getLastCommitSha(cacheKey)

        if (lastCommitSha.isNullOrEmpty()) {
            return emptyList()
        }

        val lastIndex = commits.indexOfFirst { it.ref == lastCommitSha }
        if (lastIndex == -1) {
            return commits
        }

        return commits.take(lastIndex)
    }

    private fun updateCache(cacheKey: String, comments: List<Comment>, events: List<Event>, commits: List<Commit>) {
        if (comments.isNotEmpty()) {
            val maxCommentId = comments.maxByOrNull { it.id }!!.id
            eventStateCache.setLastCommentId(cacheKey, maxCommentId)
            comments.forEach { comment ->
                eventStateCache.setCommentUpdatedAt("$cacheKey:comment:${comment.id}", comment.updatedAt)
            }
        } else if (eventStateCache.getLastCommentId(cacheKey) == null) {
            eventStateCache.setLastCommentId(cacheKey, 0L)
        }

        if (events.isNotEmpty()) {
            val maxEventId = events.maxByOrNull { it.id }!!.id
            eventStateCache.setLastEventId(cacheKey, maxEventId)
            events.forEach { event ->
                eventStateCache.setEventCreatedAt("$cacheKey:event:${event.id}", event.updatedAt)
            }
        } else if (eventStateCache.getLastEventId(cacheKey) == null) {
            eventStateCache.setLastEventId(cacheKey, 0L)
        }

        if (commits.isNotEmpty()) {
            eventStateCache.setLastCommitSha(cacheKey, commits.first().ref)
        } else if (eventStateCache.getLastCommitSha(cacheKey) == null) {
            eventStateCache.setLastCommitSha(cacheKey, "")
        }
    }
}
