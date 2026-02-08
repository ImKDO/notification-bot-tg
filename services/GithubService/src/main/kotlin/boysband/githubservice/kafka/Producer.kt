package boysband.githubservice.kafka

import boysband.githubservice.model.response.*
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class Producer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val TOPIC_ISSUE = "github_issue_events"
        const val TOPIC_COMMIT = "github_commit_events"
        const val TOPIC_PULL_REQUEST = "github_pull_request_events"
        const val TOPIC_BRANCH = "github_branch_events"
        const val TOPIC_GITHUB_ACTIONS = "github_actions_events"
    }

    fun send(chatId: Long, response: GithubEventResponse) {
        when (response) {
            is IssueEventResponse -> sendIssueEvent(chatId, response)
            is CommitEventResponse -> sendCommitEvent(chatId, response)
            is PullRequestEventResponse -> sendPullRequestEvent(chatId, response)
            is BranchEventResponse -> sendBranchEvent(chatId, response)
            is GithubActionsEventResponse -> sendGithubActionsEvent(chatId, response)
        }
    }

    fun sendIssueEvent(chatId: Long, response: IssueEventResponse) {
        val message = IssueNotification(
            chatId = chatId,
            issueNumber = response.issue.issueNumber,
            title = response.issue.title,
            state = response.issue.state,
            htmlUrl = response.issue.htmlUrl,
            owner = response.issue.owner,
            repo = response.issue.repo,
            labels = response.issue.labels.map { LabelNotification(it.name, it.color, it.description) },
            milestone = response.issue.milestone?.let { MilestoneNotification(it.title, it.state, it.dueOn) },
            assignees = response.issue.assignees.map { it.name },
            newComments = response.newComments.map { CommentNotification(it.author.name, it.body, it.link) },
            updatedComments = response.updatedComments.map { CommentNotification(it.author.name, it.body, it.link) },
            newEvents = response.newEvents.map { EventNotification(it.event, it.actor.name, it.createdAt) },
            updatedEvents = response.updatedEvents.map { EventNotification(it.event, it.actor.name, it.updatedAt) }
        )

        logger.info("Sending Issue event to topic $TOPIC_ISSUE: Issue #${response.issue.issueNumber}")
        kafkaTemplate.send(TOPIC_ISSUE, chatId.toString(), message)
            .whenComplete { result, ex ->
                if (ex != null) {
                    logger.error("Failed to send Issue event: ${ex.message}")
                } else {
                    logger.debug("Issue event sent successfully: offset=${result.recordMetadata.offset()}")
                }
            }
    }

    fun sendCommitEvent(chatId: Long, response: CommitEventResponse) {
        val message = CommitNotification(
            chatId = chatId,
            sha = response.commit.ref,
            message = response.commit.commitInfo.message,
            author = response.commit.author.name,
            htmlUrl = response.commit.htmlUrl,
            owner = response.commit.owner,
            repo = response.commit.repo,
            newComments = response.newComments.map { CommentNotification(it.author.name, it.body, it.link) },
            updatedComments = response.updatedComments.map { CommentNotification(it.author.name, it.body, it.link) }
        )

        logger.info("Sending Commit event to topic $TOPIC_COMMIT: ${response.commit.ref.take(7)}")
        kafkaTemplate.send(TOPIC_COMMIT, chatId.toString(), message)
            .whenComplete { result, ex ->
                if (ex != null) {
                    logger.error("Failed to send Commit event: ${ex.message}")
                } else {
                    logger.debug("Commit event sent successfully: offset=${result.recordMetadata.offset()}")
                }
            }
    }

    fun sendPullRequestEvent(chatId: Long, response: PullRequestEventResponse) {
        val message = PullRequestNotification(
            chatId = chatId,
            prNumber = response.pullRequest.prNumber,
            title = response.pullRequest.title,
            state = response.pullRequest.state,
            htmlUrl = response.pullRequest.htmlUrl,
            owner = response.pullRequest.owner,
            repo = response.pullRequest.repo,
            headBranch = response.pullRequest.head.ref,
            baseBranch = response.pullRequest.base.ref,
            draft = response.pullRequest.draft,
            labels = response.pullRequest.labels.map { LabelNotification(it.name, it.color, it.description) },
            milestone = response.pullRequest.milestone?.let { MilestoneNotification(it.title, it.state, it.dueOn) },
            assignees = response.pullRequest.assignees.map { it.name },
            reviewers = response.pullRequest.requestedReviewers.map { it.name },
            newComments = response.newComments.map { CommentNotification(it.author.name, it.body, it.link) },
            updatedComments = response.updatedComments.map { CommentNotification(it.author.name, it.body, it.link) },
            newEvents = response.newEvents.map { EventNotification(it.event, it.actor.name, it.createdAt) },
            updatedEvents = response.updatedEvents.map { EventNotification(it.event, it.actor.name, it.updatedAt) },
            newCommits = response.newCommits.map {
                CommitShortNotification(it.ref, it.commitInfo.message, it.author.name)
            }
        )

        logger.info("Sending PR event to topic $TOPIC_PULL_REQUEST: PR #${response.pullRequest.prNumber}")
        kafkaTemplate.send(TOPIC_PULL_REQUEST, chatId.toString(), message)
            .whenComplete { result, ex ->
                if (ex != null) {
                    logger.error("Failed to send PR event: ${ex.message}")
                } else {
                    logger.debug("PR event sent successfully: offset=${result.recordMetadata.offset()}")
                }
            }
    }

    fun sendBranchEvent(chatId: Long, response: BranchEventResponse) {
        val message = BranchNotification(
            chatId = chatId,
            owner = response.branch.owner,
            repo = response.branch.repo,
            branchName = response.branch.name,
            newCommits = response.newCommits.map {
                CommitShortNotification(
                    it.ref,
                    it.commitInfo.message,
                    it.commitInfo.authorInfo.name
                )
            }
        )

        logger.info("Sending Branch event to topic $TOPIC_BRANCH: ${response.branch.name}")
        kafkaTemplate.send(TOPIC_BRANCH, chatId.toString(), message)
            .whenComplete { result, ex ->
                if (ex != null) {
                    logger.error("Failed to send Branch event: ${ex.message}")
                } else {
                    logger.debug("Branch event sent successfully: offset=${result.recordMetadata.offset()}")
                }
            }
    }

    fun sendGithubActionsEvent(chatId: Long, response: GithubActionsEventResponse) {
        val message = GithubActionsNotification(
            chatId = chatId,
            owner = response.githubActions.owner,
            repo = response.githubActions.repo,
            workflowId = response.githubActions.workflowId,
            newRuns = response.newRuns.map {
                WorkflowRunNotification(
                    id = it.id,
                    runNumber = it.runNumber,
                    name = it.name,
                    status = it.status,
                    conclusion = it.conclusion,
                    htmlUrl = it.htmlUrl,
                    headBranch = it.headBranch,
                    event = it.event,
                    actor = it.actor.name
                )
            },
            updatedRuns = response.updatedRuns.map {
                WorkflowRunNotification(
                    id = it.id,
                    runNumber = it.runNumber,
                    name = it.name,
                    status = it.status,
                    conclusion = it.conclusion,
                    htmlUrl = it.htmlUrl,
                    headBranch = it.headBranch,
                    event = it.event,
                    actor = it.actor.name
                )
            }
        )

        logger.info("Sending GitHub Actions event to topic $TOPIC_GITHUB_ACTIONS")
        kafkaTemplate.send(TOPIC_GITHUB_ACTIONS, chatId.toString(), message)
            .whenComplete { result, ex ->
                if (ex != null) {
                    logger.error("Failed to send GitHub Actions event: ${ex.message}")
                } else {
                    logger.debug("GitHub Actions event sent successfully: offset=${result.recordMetadata.offset()}")
                }
            }
    }
}

data class CommentNotification(
    val author: String,
    val body: String,
    val link: String
)

data class EventNotification(
    val event: String,
    val actor: String,
    val timestamp: String
)

data class CommitShortNotification(
    val sha: String,
    val message: String,
    val author: String
)

data class LabelNotification(
    val name: String,
    val color: String,
    val description: String?
)

data class MilestoneNotification(
    val title: String,
    val state: String,
    val dueOn: String?
)

data class IssueNotification(
    val chatId: Long,
    val issueNumber: Int,
    val title: String,
    val state: String,
    val htmlUrl: String,
    val owner: String,
    val repo: String,
    val labels: List<LabelNotification>,
    val milestone: MilestoneNotification?,
    val assignees: List<String>,
    val newComments: List<CommentNotification>,
    val updatedComments: List<CommentNotification>,
    val newEvents: List<EventNotification>,
    val updatedEvents: List<EventNotification>
)

data class CommitNotification(
    val chatId: Long,
    val sha: String,
    val message: String,
    val author: String,
    val htmlUrl: String,
    val owner: String,
    val repo: String,
    val newComments: List<CommentNotification>,
    val updatedComments: List<CommentNotification>
)

data class PullRequestNotification(
    val chatId: Long,
    val prNumber: Int,
    val title: String,
    val state: String,
    val htmlUrl: String,
    val owner: String,
    val repo: String,
    val headBranch: String,
    val baseBranch: String,
    val draft: Boolean,
    val labels: List<LabelNotification>,
    val milestone: MilestoneNotification?,
    val assignees: List<String>,
    val reviewers: List<String>,
    val newComments: List<CommentNotification>,
    val updatedComments: List<CommentNotification>,
    val newEvents: List<EventNotification>,
    val updatedEvents: List<EventNotification>,
    val newCommits: List<CommitShortNotification>
)

data class BranchNotification(
    val chatId: Long,
    val owner: String,
    val repo: String,
    val branchName: String,
    val newCommits: List<CommitShortNotification>
)

data class GithubActionsNotification(
    val chatId: Long,
    val owner: String,
    val repo: String,
    val workflowId: String,
    val newRuns: List<WorkflowRunNotification>,
    val updatedRuns: List<WorkflowRunNotification>
)

data class WorkflowRunNotification(
    val id: Long,
    val runNumber: Int,
    val name: String,
    val status: String,
    val conclusion: String?,
    val htmlUrl: String,
    val headBranch: String,
    val event: String,
    val actor: String
)
