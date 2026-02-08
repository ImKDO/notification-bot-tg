package boysband.githubservice.service

import boysband.githubservice.cache.EventStateCache
import boysband.githubservice.model.resourse.Issue
import boysband.githubservice.model.response.IssueEventResponse
import boysband.githubservice.model.utils.Comment
import boysband.githubservice.model.utils.Event
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.body

@Service
class IssueProcessing(
    private val utilsProcessing: UtilsProcessing,
    private val eventStateCache: EventStateCache
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun processIssue(chatId: Long, issue: Issue, token: String): IssueEventResponse {
        val issueDetails = getIssueDetails(issue, token) ?: issue
        val comments = getComments(issue, token) ?: emptyList()
        val events = getEvents(issue, token) ?: emptyList()

        val cacheKey = eventStateCache.buildKey(chatId, issue.owner, issue.repo, "issue", issue.issueNumber.toString())

        val (newComments, updatedComments) = filterNewAndUpdatedComments(cacheKey, comments)
        val (newEvents, updatedEvents) = filterNewAndUpdatedEvents(cacheKey, events)

        updateCache(cacheKey, comments, events)

        return IssueEventResponse(
            issue = issueDetails,
            newComments = newComments,
            updatedComments = updatedComments,
            newEvents = newEvents,
            updatedEvents = updatedEvents
        )
    }

    fun getIssueDetails(issue: Issue, token: String): Issue? {
        return utilsProcessing.baseGithubRequestUrl(issue, "", token)
            ?.onStatus({ it.isError }) { _, response ->
                logger.error("Error getting issue details: ${response.statusCode}")
            }
            ?.body<Issue>()
            ?.copy(owner = issue.owner, repo = issue.repo)
    }

    fun getComments(issue: Issue, token: String): List<Comment>? {
        return utilsProcessing.baseGithubRequestUrl(issue, "/comments", token)
            ?.onStatus({ it.isError }) { _, response ->
                logger.error("Error getting issue comments: ${response.statusCode}")
            }
            ?.body<List<Comment>>()
    }

    fun getEvents(issue: Issue, token: String): List<Event>? {
        return utilsProcessing.baseGithubRequestUrl(issue, "/events", token)
            ?.onStatus({ it.isError }) { _, response ->
                logger.error("Error getting issue events: ${response.statusCode}")
            }
            ?.body<List<Event>>()
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

    private fun updateCache(cacheKey: String, comments: List<Comment>, events: List<Event>) {
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
    }
}