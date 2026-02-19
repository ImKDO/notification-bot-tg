package boysband.githubservice.cache

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Кеш для хранения состояния отслеживаемых ресурсов в Redis.
 * Ключ - уникальный идентификатор ресурса (chatId:owner:repo:type:id)
 * Значение - состояние ресурса (последние ID событий, комментариев и т.д.)
 */
@Component
class EventStateCache(
    private val redisTemplate: StringRedisTemplate
) {

    companion object {
        private const val PREFIX = "github:cache:"
        private val TTL: Duration = Duration.ofDays(7)
    }

    // ── Last Event ID ────────────────────────────────────────────────────────

    fun getLastEventId(key: String): Long? =
        redisTemplate.opsForValue().get("${PREFIX}event_id:$key")?.toLongOrNull()

    fun setLastEventId(key: String, id: Long) {
        redisTemplate.opsForValue().set("${PREFIX}event_id:$key", id.toString(), TTL)
    }

    // ── Last Comment ID ──────────────────────────────────────────────────────

    fun getLastCommentId(key: String): Long? =
        redisTemplate.opsForValue().get("${PREFIX}comment_id:$key")?.toLongOrNull()

    fun setLastCommentId(key: String, id: Long) {
        redisTemplate.opsForValue().set("${PREFIX}comment_id:$key", id.toString(), TTL)
    }

    // ── Last Commit SHA ──────────────────────────────────────────────────────

    fun getLastCommitSha(key: String): String? =
        redisTemplate.opsForValue().get("${PREFIX}commit_sha:$key")

    fun setLastCommitSha(key: String, sha: String) {
        redisTemplate.opsForValue().set("${PREFIX}commit_sha:$key", sha, TTL)
    }

    // ── Last Workflow Run ID ─────────────────────────────────────────────────

    fun getLastWorkflowRunId(key: String): Long? =
        redisTemplate.opsForValue().get("${PREFIX}workflow_run_id:$key")?.toLongOrNull()

    fun setLastWorkflowRunId(key: String, id: Long) {
        redisTemplate.opsForValue().set("${PREFIX}workflow_run_id:$key", id.toString(), TTL)
    }

    // ── Comment Updated At ───────────────────────────────────────────────────

    fun getCommentUpdatedAt(key: String): String? =
        redisTemplate.opsForValue().get("${PREFIX}comment_updated:$key")

    fun setCommentUpdatedAt(key: String, updatedAt: String) {
        redisTemplate.opsForValue().set("${PREFIX}comment_updated:$key", updatedAt, TTL)
    }

    // ── Event Created At ─────────────────────────────────────────────────────

    fun getEventCreatedAt(key: String): String? =
        redisTemplate.opsForValue().get("${PREFIX}event_created:$key")

    fun setEventCreatedAt(key: String, createdAt: String) {
        redisTemplate.opsForValue().set("${PREFIX}event_created:$key", createdAt, TTL)
    }

    // ── Workflow Run Status ──────────────────────────────────────────────────

    fun getWorkflowRunStatus(key: String): String? =
        redisTemplate.opsForValue().get("${PREFIX}workflow_status:$key")

    fun setWorkflowRunStatus(key: String, status: String) {
        redisTemplate.opsForValue().set("${PREFIX}workflow_status:$key", status, TTL)
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    fun buildKey(chatId: Long, owner: String, repo: String, type: String, resourceId: String): String {
        return "$chatId:$owner:$repo:$type:$resourceId"
    }

    fun clearState(key: String) {
        val keysToDelete = listOf(
            "${PREFIX}event_id:$key",
            "${PREFIX}comment_id:$key",
            "${PREFIX}commit_sha:$key",
            "${PREFIX}workflow_run_id:$key",
            "${PREFIX}comment_updated:$key",
            "${PREFIX}event_created:$key",
            "${PREFIX}workflow_status:$key",
        )
        redisTemplate.delete(keysToDelete)
    }
}

