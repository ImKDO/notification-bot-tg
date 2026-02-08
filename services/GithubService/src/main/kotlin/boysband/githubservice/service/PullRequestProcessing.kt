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
        return utilsProcessing.baseGithubRequestUrl(pullRequest, "", token)
            ?.onStatus({ it.isError }) { _, response ->
                logger.error("Error getting PR details: ${response.statusCode}")
            }
            ?.body<PullRequest>()
            ?.copy(owner = pullRequest.owner, repo = pullRequest.repo)
    }

    fun getComments(pullRequest: PullRequest, token: String): List<Comment>? {
        return utilsProcessing.baseGithubRequestUrl(pullRequest, "/comments", token)
            ?.onStatus({ it.isError }) { _, response ->
                logger.error("Error getting PR comments: ${response.statusCode}")
            }
            ?.body<List<Comment>>()
    }

    fun getReviewComments(pullRequest: PullRequest, token: String): List<Comment>? {
        return utilsProcessing.baseGithubRequestUrl(pullRequest, "/reviews", token)
            ?.onStatus({ it.isError }) { _, response ->
                logger.error("Error getting PR review comments: ${response.statusCode}")
            }
            ?.body<List<Comment>>()
    }

    fun getEvents(pullRequest: PullRequest, token: String): List<Event>? {
        return utilsProcessing.baseGithubRequestUrl(
            pullRequest.copy(prNumber = pullRequest.prNumber),
            "/events",
            token
        )
            ?.onStatus({ it.isError }) { _, response ->
                logger.error("Error getting PR events: ${response.statusCode}")
            }
            ?.body<List<Event>>()
    }

    fun getCommits(pullRequest: PullRequest, token: String): List<Commit>? {
        return utilsProcessing.baseGithubRequestUrl(pullRequest, "/commits", token)
            ?.onStatus({ it.isError }) { _, response ->
                logger.error("Error getting PR commits: ${response.statusCode}")
            }
            ?.body<List<Commit>>()
            ?.map { it.copy(owner = pullRequest.owner, repo = pullRequest.repo) }
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

        if (lastCommitSha == null) {
            return emptyList()
        }

        val lastIndex = commits.indexOfFirst { it.ref == lastCommitSha }
        if (lastIndex == -1) {
            return commits
        }

        return commits.take(lastIndex)
    }

    private fun updateCache(cacheKey: String, comments: List<Comment>, events: List<Event>, commits: List<Commit>) {
        comments.maxByOrNull { it.id }?.let {
            eventStateCache.setLastCommentId(cacheKey, it.id)
        }

        comments.forEach { comment ->
            eventStateCache.setCommentUpdatedAt("$cacheKey:comment:${comment.id}", comment.updatedAt)
        }

        events.maxByOrNull { it.id }?.let {
            eventStateCache.setLastEventId(cacheKey, it.id)
        }

        events.forEach { event ->
            eventStateCache.setEventCreatedAt("$cacheKey:event:${event.id}", event.updatedAt)
        }

        commits.firstOrNull()?.let {
            eventStateCache.setLastCommitSha(cacheKey, it.ref)
        }
    }
}
