package boysband.coreservice.service

import boysband.coreservice.client.DbServiceClient
import boysband.coreservice.dto.Notification
import boysband.coreservice.kafka.NotificationProducer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class UpdateHandlerService(
    private val notificationProducer: NotificationProducer,
    private val dbServiceClient: DbServiceClient,
) {

    private val objectMapper = jacksonObjectMapper()

    suspend fun handleUpdate(topic: String, payload: String) {
        when (topic) {
            "updates" -> handleStackOverflowUpdate(payload)
            "github_issue_events" -> handleGithubIssue(payload)
            "github_commit_events" -> handleGithubCommit(payload)
            "github_pull_request_events" -> handleGithubPullRequest(payload)
            "github_branch_events" -> handleGithubBranch(payload)
            "github_actions_events" -> handleGithubActions(payload)
            else -> logger.warn("Unknown topic: $topic")
        }
    }

    private suspend fun handleStackOverflowUpdate(payload: String) {
        val node = parse(payload) ?: return

        val author = node.path("author").asText("")
        val text = node.path("text").asText("")
        val link = node.path("link").asText("")
        val type = stackOverflowType(node.path("type"))
        val actionId = node.path("actionId").asInt(0)
        val chatId = node.path("chatId").asLong(0L)
        val creationDateRaw = node.path("creationDate").asText("")
        val creationDate = ZonedDateTime.parse(creationDateRaw)

        if (chatId == 0L || actionId == 0 || creationDateRaw.isBlank()) {
            logger.error("Invalid StackOverflow update (chatId/actionId/creationDate missing): $payload")
            return
        }

        if (link.isBlank()) {
            logger.warn("StackOverflow update without link: $payload")
            return
        }

        val notification = Notification(
            chatId = chatId,
            title = if (author.isNotBlank()) "StackOverflow: $author" else "StackOverflow update",
            message = text,
            service = "stackoverflow",
            type = type,
            url = link,
        )
        notificationProducer.sendNotification(notification)

        val actions = dbServiceClient.getActions()
        val action = actions.firstOrNull { it.id == actionId }
        if (action == null) {
            logger.error("Action not found for StackOverflow update: actionId=$actionId")
            return
        }

        try {
            dbServiceClient.updateAction(actionId, action.copy(lastCheckDate = creationDate))
        } catch (e: Exception) {
            logger.error("Failed to update lastCheckDate for actionId=$actionId", e)
        }
    }

    private fun stackOverflowType(typeNode: JsonNode): String {
        if (typeNode.isMissingNode || typeNode.isNull) return "UNKNOWN"
        if (typeNode.isTextual) return typeNode.asText("UNKNOWN")

        return when (val raw = typeNode.toString()) {
            "{}", "[]", "null", "\"\"" -> "UNKNOWN"
            else -> raw
        }
    }

    private fun handleGithubIssue(payload: String) {
        val node = parse(payload) ?: return
        val chatId = node.path("chatId").asLong(0L)
        val issueNumber = node.path("issueNumber").asInt(0)
        val title = node.path("title").asText("")
        val htmlUrl = node.path("htmlUrl").asText("")
        val owner = node.path("owner").asText("")
        val repo = node.path("repo").asText("")
        val newComments = node.path("newComments").sizeOrZero()
        val updatedComments = node.path("updatedComments").sizeOrZero()
        val newEvents = node.path("newEvents").sizeOrZero()
        val updatedEvents = node.path("updatedEvents").sizeOrZero()

        if (chatId == 0L) {
            logger.warn("GitHub issue notification without chatId: $payload")
            return
        }

        val message = buildString {
            append("$owner/$repo\n")
            append("Issue #$issueNumber\n")
            if (newComments + updatedComments + newEvents + updatedEvents > 0) {
                append("New comments: $newComments, updated comments: $updatedComments\n")
                append("New events: $newEvents, updated events: $updatedEvents")
            }
        }.trim()

        notificationProducer.sendNotification(
            Notification(
                chatId = chatId,
                title = "GitHub Issue #$issueNumber${if (title.isNotBlank()) ": $title" else ""}",
                message = message,
                service = "github",
                type = "issue",
                url = htmlUrl,
            )
        )
    }

    private fun handleGithubCommit(payload: String) {
        val node = parse(payload) ?: return
        val chatId = node.path("chatId").asLong(0L)
        val sha = node.path("sha").asText("")
        val message = node.path("message").asText("")
        val author = node.path("author").asText("")
        val htmlUrl = node.path("htmlUrl").asText("")
        val owner = node.path("owner").asText("")
        val repo = node.path("repo").asText("")

        if (chatId == 0L) {
            logger.warn("GitHub commit notification without chatId: $payload")
            return
        }

        val title = "GitHub Commit ${sha.takeIf { it.isNotBlank() }?.take(7) ?: ""}".trim()
        val body = buildString {
            append("$owner/$repo\n")
            if (author.isNotBlank()) append("Author: $author\n")
            append(message)
        }.trim()

        notificationProducer.sendNotification(
            Notification(
                chatId = chatId,
                title = title,
                message = body,
                service = "github",
                type = "commit",
                url = htmlUrl,
            )
        )
    }

    private fun handleGithubPullRequest(payload: String) {
        val node = parse(payload) ?: return
        val chatId = node.path("chatId").asLong(0L)
        val prNumber = node.path("prNumber").asInt(0)
        val title = node.path("title").asText("")
        val state = node.path("state").asText("")
        val htmlUrl = node.path("htmlUrl").asText("")
        val owner = node.path("owner").asText("")
        val repo = node.path("repo").asText("")

        if (chatId == 0L) {
            logger.warn("GitHub PR notification without chatId: $payload")
            return
        }

        val body = buildString {
            append("$owner/$repo\n")
            append("PR #$prNumber")
            if (state.isNotBlank()) append(" ($state)")
        }

        notificationProducer.sendNotification(
            Notification(
                chatId = chatId,
                title = "GitHub PR #$prNumber${if (title.isNotBlank()) ": $title" else ""}",
                message = body,
                service = "github",
                type = "pull_request",
                url = htmlUrl,
            )
        )
    }

    private fun handleGithubBranch(payload: String) {
        val node = parse(payload) ?: return
        val chatId = node.path("chatId").asLong(0L)
        val owner = node.path("owner").asText("")
        val repo = node.path("repo").asText("")
        val branchName = node.path("branchName").asText("")
        val newCommits = node.path("newCommits").sizeOrZero()

        if (chatId == 0L) {
            logger.warn("GitHub branch notification without chatId: $payload")
            return
        }

        val body = buildString {
            append("$owner/$repo\n")
            append("Branch: $branchName\n")
            append("New commits: $newCommits")
        }.trim()

        notificationProducer.sendNotification(
            Notification(
                chatId = chatId,
                title = "GitHub Branch${if (branchName.isNotBlank()) ": $branchName" else ""}",
                message = body,
                service = "github",
                type = "branch",
                url = "",
            )
        )
    }

    private fun handleGithubActions(payload: String) {
        val node = parse(payload) ?: return
        val chatId = node.path("chatId").asLong(0L)
        val owner = node.path("owner").asText("")
        val repo = node.path("repo").asText("")
        val workflowId = node.path("workflowId").asText("")
        val newRuns = node.path("newRuns").sizeOrZero()
        val updatedRuns = node.path("updatedRuns").sizeOrZero()

        if (chatId == 0L) {
            logger.warn("GitHub actions notification without chatId: $payload")
            return
        }

        val body = buildString {
            append("$owner/$repo\n")
            if (workflowId.isNotBlank()) append("Workflow: $workflowId\n")
            append("New runs: $newRuns, updated runs: $updatedRuns")
        }.trim()

        notificationProducer.sendNotification(
            Notification(
                chatId = chatId,
                title = "GitHub Actions${if (workflowId.isNotBlank()) ": $workflowId" else ""}",
                message = body,
                service = "github",
                type = "actions",
                url = "",
            )
        )
    }

    private fun parse(payload: String): JsonNode? {
        return try {
            objectMapper.readTree(payload)
        } catch (e: Exception) {
            logger.error("Failed to parse JSON payload: $payload", e)
            null
        }
    }

    private fun JsonNode.sizeOrZero(): Int = if (this.isArray) this.size() else 0

    companion object {
        private val logger = LoggerFactory.getLogger(UpdateHandlerService::class.java)
    }
}