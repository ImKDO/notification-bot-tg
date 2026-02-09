package boysband.githubservice.service

import boysband.githubservice.cache.EventStateCache
import boysband.githubservice.model.response.CommitEventResponse
import boysband.githubservice.model.utils.Comment
import boysband.githubservice.model.resourse.Commit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.body

@Service
class CommitProcessing(
    private val utilsProcessing: UtilsProcessing,
    private val eventStateCache: EventStateCache
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun processCommit(chatId: Long, commit: Commit, token: String): CommitEventResponse {
        val commitDetails = getCommitDetails(commit, token) ?: commit
        val comments = getComments(commit, token) ?: emptyList()

        val cacheKey = eventStateCache.buildKey(chatId, commit.owner, commit.repo, "commit", commit.ref)

        val (newComments, updatedComments) = filterNewAndUpdatedComments(cacheKey, comments)

        // Обновляем кеш
        updateCache(cacheKey, comments)

        return CommitEventResponse(
            commit = commitDetails,
            newComments = newComments,
            updatedComments = updatedComments
        )
    }

    fun getCommitDetails(commit: Commit, token: String): Commit? {
        return try {
            utilsProcessing.baseGithubRequestUrl(commit, "", token)
                ?.onStatus({ it.isError }) { _, response ->
                    throw RuntimeException("GitHub API error ${response.statusCode} for commit details")
                }
                ?.body<Commit>()
                ?.copy(owner = commit.owner, repo = commit.repo)
        } catch (e: Exception) {
            logger.error("Failed to fetch commit details for ${commit.owner}/${commit.repo}@${commit.ref.take(7)}", e)
            null
        }
    }

    fun getComments(commit: Commit, token: String): List<Comment>? {
        return try {
            utilsProcessing.baseGithubRequestUrl(commit, "/comments", token)
                ?.onStatus({ it.isError }) { _, response ->
                    throw RuntimeException("GitHub API error ${response.statusCode} for commit comments")
                }
                ?.body<List<Comment>>()
        } catch (e: Exception) {
            logger.error("Failed to fetch commit comments for ${commit.owner}/${commit.repo}@${commit.ref.take(7)}", e)
            null
        }
    }

    private fun filterNewAndUpdatedComments(cacheKey: String, comments: List<Comment>): Pair<List<Comment>, List<Comment>> {
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

    private fun updateCache(cacheKey: String, comments: List<Comment>) {
        if (comments.isNotEmpty()) {
            val maxCommentId = comments.maxByOrNull { it.id }!!.id
            eventStateCache.setLastCommentId(cacheKey, maxCommentId)
            comments.forEach { comment ->
                eventStateCache.setCommentUpdatedAt("$cacheKey:comment:${comment.id}", comment.updatedAt)
            }
        } else if (eventStateCache.getLastCommentId(cacheKey) == null) {
            eventStateCache.setLastCommentId(cacheKey, 0L)
        }
    }
}