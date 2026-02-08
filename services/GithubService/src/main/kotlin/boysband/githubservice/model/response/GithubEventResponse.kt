package boysband.githubservice.model.response

import boysband.githubservice.model.resourse.*
import boysband.githubservice.model.utils.Comment
import boysband.githubservice.model.utils.Event

/**
 * Унифицированный ответ с новыми событиями для всех типов ресурсов
 */
sealed class GithubEventResponse {
    abstract val hasNewEvents: Boolean
}

data class IssueEventResponse(
    val issue: Issue,
    val newComments: List<Comment> = emptyList(),
    val updatedComments: List<Comment> = emptyList(),
    val newEvents: List<Event> = emptyList(),
    val updatedEvents: List<Event> = emptyList(),
    override val hasNewEvents: Boolean = newComments.isNotEmpty() || updatedComments.isNotEmpty() || newEvents.isNotEmpty() || updatedEvents.isNotEmpty()
) : GithubEventResponse()

data class CommitEventResponse(
    val commit: Commit,
    val newComments: List<Comment> = emptyList(),
    val updatedComments: List<Comment> = emptyList(),
    override val hasNewEvents: Boolean = newComments.isNotEmpty() || updatedComments.isNotEmpty()
) : GithubEventResponse()

data class PullRequestEventResponse(
    val pullRequest: PullRequest,
    val newComments: List<Comment> = emptyList(),
    val updatedComments: List<Comment> = emptyList(),
    val newEvents: List<Event> = emptyList(),
    val updatedEvents: List<Event> = emptyList(),
    val newCommits: List<Commit> = emptyList(),
    override val hasNewEvents: Boolean = newComments.isNotEmpty() || updatedComments.isNotEmpty() || newEvents.isNotEmpty() || updatedEvents.isNotEmpty() || newCommits.isNotEmpty()
) : GithubEventResponse()

data class BranchEventResponse(
    val branch: Branch,
    val newCommits: List<Commit> = emptyList(),
    override val hasNewEvents: Boolean = newCommits.isNotEmpty()
) : GithubEventResponse()

data class GithubActionsEventResponse(
    val githubActions: GithubActions,
    val newRuns: List<WorkflowRun> = emptyList(),
    val updatedRuns: List<WorkflowRun> = emptyList(),
    override val hasNewEvents: Boolean = newRuns.isNotEmpty() || updatedRuns.isNotEmpty()
) : GithubEventResponse()
