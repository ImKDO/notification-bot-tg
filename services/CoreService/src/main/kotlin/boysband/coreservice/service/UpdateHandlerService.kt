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
        val state = node.path("state").asText("")
        val htmlUrl = node.path("htmlUrl").asText("")
        val owner = node.path("owner").asText("")
        val repo = node.path("repo").asText("")

        if (chatId == 0L) {
            logger.warn("GitHub issue notification without chatId: $payload")
            return
        }

        val message = buildString {
            append("📦 $owner/$repo\n")
            append("🔢 Issue #$issueNumber")
            if (state.isNotBlank()) append(" ($state)")
            append("\n\n")

            appendComments("💬 Новые комментарии", node.path("newComments"))
            appendComments("✏️ Обновлённые комментарии", node.path("updatedComments"))
            appendEvents("📌 Новые события", node.path("newEvents"))
            appendEvents("🔄 Обновлённые события", node.path("updatedEvents"))
        }.trim()

        notificationProducer.sendNotification(
            Notification(
                chatId = chatId,
                title = "Issue #$issueNumber${if (title.isNotBlank()) ": $title" else ""}",
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
        val commitMessage = node.path("message").asText("")
        val author = node.path("author").asText("")
        val htmlUrl = node.path("htmlUrl").asText("")
        val owner = node.path("owner").asText("")
        val repo = node.path("repo").asText("")

        if (chatId == 0L) {
            logger.warn("GitHub commit notification without chatId: $payload")
            return
        }

        val shortSha = sha.takeIf { it.isNotBlank() }?.take(7) ?: ""
        val message = buildString {
            append("📦 $owner/$repo\n")
            if (author.isNotBlank()) append("👤 $author\n")
            if (commitMessage.isNotBlank()) append("📝 ${commitMessage.take(200)}\n\n")

            appendComments("💬 Новые комментарии", node.path("newComments"))
            appendComments("✏️ Обновлённые комментарии", node.path("updatedComments"))
        }.trim()

        notificationProducer.sendNotification(
            Notification(
                chatId = chatId,
                title = "Commit $shortSha",
                message = message,
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
        val headBranch = node.path("headBranch").asText("")
        val baseBranch = node.path("baseBranch").asText("")

        if (chatId == 0L) {
            logger.warn("GitHub PR notification without chatId: $payload")
            return
        }

        val message = buildString {
            append("📦 $owner/$repo\n")
            append("🔢 PR #$prNumber")
            if (state.isNotBlank()) append(" ($state)")
            append("\n")
            if (headBranch.isNotBlank() && baseBranch.isNotBlank()) {
                append("🌿 $headBranch → $baseBranch\n")
            }
            append("\n")

            appendComments("💬 Новые комментарии", node.path("newComments"))
            appendComments("✏️ Обновлённые комментарии", node.path("updatedComments"))
            appendEvents("📌 Новые события", node.path("newEvents"))
            appendEvents("🔄 Обновлённые события", node.path("updatedEvents"))
            appendShortCommits("📝 Новые коммиты", node.path("newCommits"))
        }.trim()

        notificationProducer.sendNotification(
            Notification(
                chatId = chatId,
                title = "PR #$prNumber${if (title.isNotBlank()) ": $title" else ""}",
                message = message,
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

        if (chatId == 0L) {
            logger.warn("GitHub branch notification without chatId: $payload")
            return
        }

        val message = buildString {
            append("📦 $owner/$repo\n")
            append("🌿 Branch: $branchName\n\n")
            appendShortCommits("📝 Новые коммиты", node.path("newCommits"))
        }.trim()

        notificationProducer.sendNotification(
            Notification(
                chatId = chatId,
                title = "Branch${if (branchName.isNotBlank()) ": $branchName" else ""}",
                message = message,
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

        if (chatId == 0L) {
            logger.warn("GitHub actions notification without chatId: $payload")
            return
        }

        val message = buildString {
            append("📦 $owner/$repo\n")
            if (workflowId.isNotBlank()) append("⚙️ Workflow: $workflowId\n")
            append("\n")
            appendWorkflowRuns("🆕 Новые запуски", node.path("newRuns"))
            appendWorkflowRuns("🔄 Обновлённые запуски", node.path("updatedRuns"))
        }.trim()

        notificationProducer.sendNotification(
            Notification(
                chatId = chatId,
                title = "GitHub Actions${if (workflowId.isNotBlank()) ": $workflowId" else ""}",
                message = message,
                service = "github",
                type = "actions",
                url = "",
            )
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun StringBuilder.appendComments(header: String, comments: JsonNode) {
        if (!comments.isArray || comments.isEmpty) return
        append("$header (${comments.size()}):\n")
        comments.take(5).forEachIndexed { index, c ->
            val author = c.path("author").asText("?")
            val authorUrl = c.path("authorUrl").asText("")
            val body = c.path("body").asText("").take(120)
            val link = c.path("link").asText("")

            val authorDisplay = if (authorUrl.isNotBlank()) "<a href='$authorUrl'>$author</a>" else author
            val commentNum = "#${index + 1}"
            val commentLink = if (link.isNotBlank()) "<a href='$link'>$commentNum</a>" else commentNum

            append("  $commentLink \u2014 $authorDisplay:\n")
            append("    $body\n")
        }
        if (comments.size() > 5) append("  … и ещё ${comments.size() - 5}\n")
        append("\n")
    }

    private fun StringBuilder.appendEvents(header: String, events: JsonNode) {
        if (!events.isArray || events.isEmpty) return
        append("$header (${events.size()}):\n")
        events.take(5).forEach { e ->
            val event = e.path("event").asText("?")
            val actor = e.path("actor").asText("?")
            append("  • $event by $actor\n")
        }
        if (events.size() > 5) append("  … и ещё ${events.size() - 5}\n")
        append("\n")
    }

    private fun StringBuilder.appendShortCommits(header: String, commits: JsonNode) {
        if (!commits.isArray || commits.isEmpty) return
        append("$header (${commits.size()}):\n")
        commits.take(5).forEach { c ->
            val sha = c.path("sha").asText("").take(7)
            val msg = c.path("message").asText("").take(80)
            val author = c.path("author").asText("")
            append("  • $sha ${msg}${if (author.isNotBlank()) " ($author)" else ""}\n")
        }
        if (commits.size() > 5) append("  … и ещё ${commits.size() - 5}\n")
        append("\n")
    }

    private fun StringBuilder.appendWorkflowRuns(header: String, runs: JsonNode) {
        if (!runs.isArray || runs.isEmpty) return
        append("$header (${runs.size()}):\n")
        runs.take(5).forEach { r ->
            val name = r.path("name").asText("?")
            val runNumber = r.path("runNumber").asInt(0)
            val status = r.path("status").asText("?")
            val conclusion = r.path("conclusion").asText("")
            val statusStr = if (conclusion.isNotBlank()) "$status/$conclusion" else status
            append("  • #$runNumber $name [$statusStr]\n")
        }
        if (runs.size() > 5) append("  … и ещё ${runs.size() - 5}\n")
        append("\n")
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