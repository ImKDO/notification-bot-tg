package boysband.githubservice.cache

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Кеш для хранения состояния отслеживаемых ресурсов.
 * Ключ - уникальный идентификатор ресурса (chatId:owner:repo:type:id)
 * Значение - состояние ресурса (последние ID событий, комментариев и т.д.)
 */
@Component
class EventStateCache {

    // Хранит последний обработанный ID события для Issue/PR
    private val lastEventId = ConcurrentHashMap<String, Long>()

    // Хранит последний обработанный ID комментария
    private val lastCommentId = ConcurrentHashMap<String, Long>()

    // Хранит последний обработанный SHA коммита для Branch
    private val lastCommitSha = ConcurrentHashMap<String, String>()

    // Хранит последний обработанный ID запуска workflow
    private val lastWorkflowRunId = ConcurrentHashMap<String, Long>()

    // Хранит updatedAt для отслеживания обновлений комментариев
    private val commentUpdatedAt = ConcurrentHashMap<String, String>()

    // Хранит createdAt для отслеживания обновлений событий (событие с тем же id но новым временем = обновление)
    private val eventCreatedAt = ConcurrentHashMap<String, String>()

    // Хранит статус workflow run для отслеживания изменений (in_progress -> completed)
    private val workflowRunStatus = ConcurrentHashMap<String, String>()

    fun getLastEventId(key: String): Long? = lastEventId[key]
    fun setLastEventId(key: String, id: Long) {
        lastEventId[key] = id
    }

    fun getLastCommentId(key: String): Long? = lastCommentId[key]
    fun setLastCommentId(key: String, id: Long) {
        lastCommentId[key] = id
    }

    fun getLastCommitSha(key: String): String? = lastCommitSha[key]
    fun setLastCommitSha(key: String, sha: String) {
        lastCommitSha[key] = sha
    }

    fun getLastWorkflowRunId(key: String): Long? = lastWorkflowRunId[key]
    fun setLastWorkflowRunId(key: String, id: Long) {
        lastWorkflowRunId[key] = id
    }

    fun getCommentUpdatedAt(key: String): String? = commentUpdatedAt[key]
    fun setCommentUpdatedAt(key: String, updatedAt: String) {
        commentUpdatedAt[key] = updatedAt
    }

    fun getEventCreatedAt(key: String): String? = eventCreatedAt[key]
    fun setEventCreatedAt(key: String, createdAt: String) {
        eventCreatedAt[key] = createdAt
    }

    fun getWorkflowRunStatus(key: String): String? = workflowRunStatus[key]
    fun setWorkflowRunStatus(key: String, status: String) {
        workflowRunStatus[key] = status
    }

    fun buildKey(chatId: Long, owner: String, repo: String, type: String, resourceId: String): String {
        return "$chatId:$owner:$repo:$type:$resourceId"
    }

    fun clearState(key: String) {
        lastEventId.remove(key)
        lastCommentId.remove(key)
        lastCommitSha.remove(key)
        lastWorkflowRunId.remove(key)
        commentUpdatedAt.remove(key)
        eventCreatedAt.remove(key)
        workflowRunStatus.remove(key)
    }
}
