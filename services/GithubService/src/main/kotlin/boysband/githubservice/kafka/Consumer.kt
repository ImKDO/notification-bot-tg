package boysband.githubservice.kafka

import boysband.githubservice.model.dto.ActionDto
import boysband.githubservice.model.response.*
import boysband.githubservice.service.GithubProcessing
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class Consumer(
    private val githubProcessing: GithubProcessing,
    private val producer: Producer
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @KafkaListener(topics = ["github_request"], groupId = "github_request_group")
    fun listen(action: ActionDto) {
        log.info("Github Consumer received: $action")

        val response = githubProcessing.getResponse(action)

        if (response == null) {
            log.warn("Failed to process request for: ${action.query}")
            return
        }

        val chatId = action.user.idTgChat
        if (response.hasNewEvents) {
            log.info("New events detected for chatId: $chatId")
            processNewEvents(chatId, response)

            // Отправляем уведомление в соответствующий топик
            producer.send(chatId, response)
        } else {
            log.debug("No new events for chatId: $chatId")
        }
    }

    private fun processNewEvents(chatId: Long, response: GithubEventResponse) {
        when (response) {
            is IssueEventResponse -> {
                log.info("=== Issue #${response.issue.issueNumber} (${response.issue.title}) ===")
                log.info("State: ${response.issue.state}, URL: ${response.issue.htmlUrl}")

                if (response.newComments.isNotEmpty()) {
                    log.info("New comments (${response.newComments.size}):")
                    response.newComments.forEach { comment ->
                        log.info("  - [${comment.author.name}]: ${comment.body.take(100)}${if (comment.body.length > 100) "..." else ""}")
                    }
                }
                if (response.updatedComments.isNotEmpty()) {
                    log.info("Updated comments (${response.updatedComments.size}):")
                    response.updatedComments.forEach { comment ->
                        log.info("  - [${comment.author.name}]: ${comment.body.take(100)}${if (comment.body.length > 100) "..." else ""}")
                    }
                }
                if (response.newEvents.isNotEmpty()) {
                    log.info("New events (${response.newEvents.size}):")
                    response.newEvents.forEach { event ->
                        log.info("  - ${event.event} by ${event.actor.name} at ${event.createdAt}")
                    }
                }
                if (response.updatedEvents.isNotEmpty()) {
                    log.info("Updated events (${response.updatedEvents.size}):")
                    response.updatedEvents.forEach { event ->
                        log.info("  - ${event.event} by ${event.actor.name} (updated at ${event.updatedAt})")
                    }
                }
            }
            is CommitEventResponse -> {
                log.info("=== Commit ${response.commit.ref.take(7)} ===")
                log.info("Message: ${response.commit.commitInfo.message.take(100)}")
                log.info("Author: ${response.commit.author.name}, URL: ${response.commit.htmlUrl}")

                if (response.newComments.isNotEmpty()) {
                    log.info("New comments (${response.newComments.size}):")
                    response.newComments.forEach { comment ->
                        log.info("  - [${comment.author.name}]: ${comment.body.take(100)}${if (comment.body.length > 100) "..." else ""}")
                    }
                }
                if (response.updatedComments.isNotEmpty()) {
                    log.info("Updated comments (${response.updatedComments.size}):")
                    response.updatedComments.forEach { comment ->
                        log.info("  - [${comment.author.name}]: ${comment.body.take(100)}${if (comment.body.length > 100) "..." else ""}")
                    }
                }
            }
            is PullRequestEventResponse -> {
                log.info("=== PR #${response.pullRequest.prNumber} (${response.pullRequest.title}) ===")
                log.info("State: ${response.pullRequest.state}, URL: ${response.pullRequest.htmlUrl}")
                log.info("Branch: ${response.pullRequest.head.ref} -> ${response.pullRequest.base.ref}")

                if (response.newComments.isNotEmpty()) {
                    log.info("New comments (${response.newComments.size}):")
                    response.newComments.forEach { comment ->
                        log.info("  - [${comment.author.name}]: ${comment.body.take(100)}${if (comment.body.length > 100) "..." else ""}")
                    }
                }
                if (response.updatedComments.isNotEmpty()) {
                    log.info("Updated comments (${response.updatedComments.size}):")
                    response.updatedComments.forEach { comment ->
                        log.info("  - [${comment.author.name}]: ${comment.body.take(100)}${if (comment.body.length > 100) "..." else ""}")
                    }
                }
                if (response.newEvents.isNotEmpty()) {
                    log.info("New events (${response.newEvents.size}):")
                    response.newEvents.forEach { event ->
                        log.info("  - ${event.event} by ${event.actor.name} at ${event.createdAt}")
                    }
                }
                if (response.updatedEvents.isNotEmpty()) {
                    log.info("Updated events (${response.updatedEvents.size}):")
                    response.updatedEvents.forEach { event ->
                        log.info("  - ${event.event} by ${event.actor.name} (updated at ${event.updatedAt})")
                    }
                }
                if (response.newCommits.isNotEmpty()) {
                    log.info("New commits (${response.newCommits.size}):")
                    response.newCommits.forEach { commit ->
                        log.info("  - ${commit.ref.take(7)}: ${commit.commitInfo.message.take(50)} by ${commit.author.name}")
                    }
                }
            }
            is BranchEventResponse -> {
                log.info("=== Branch ${response.branch.owner}/${response.branch.repo}:${response.branch.name} ===")

                if (response.newCommits.isNotEmpty()) {
                    log.info("New commits (${response.newCommits.size}):")
                    response.newCommits.forEach { commit ->
                        log.info("  - ${commit.ref.take(7)}: ${commit.commitInfo.message.take(50)} by ${commit.commitInfo.authorInfo.name}")
                    }
                }
            }
            is GithubActionsEventResponse -> {
                log.info("=== GitHub Actions ${response.githubActions.owner}/${response.githubActions.repo} ===")

                if (response.newRuns.isNotEmpty()) {
                    log.info("New workflow runs (${response.newRuns.size}):")
                    response.newRuns.forEach { run ->
                        log.info("  - Run #${run.runNumber}: ${run.name}")
                        log.info("    Status: ${run.status}/${run.conclusion ?: "in_progress"}")
                        log.info("    Branch: ${run.headBranch}, Event: ${run.event}")
                        log.info("    URL: ${run.htmlUrl}")
                    }
                }
                if (response.updatedRuns.isNotEmpty()) {
                    log.info("Updated workflow runs (${response.updatedRuns.size}):")
                    response.updatedRuns.forEach { run ->
                        log.info("  - Run #${run.runNumber}: ${run.name}")
                        log.info("    Status: ${run.status}/${run.conclusion ?: "in_progress"}")
                        log.info("    URL: ${run.htmlUrl}")
                    }
                }
            }
        }
    }
}