package boysband.githubservice.service

import boysband.githubservice.model.dto.ActionDto
import boysband.githubservice.model.enums.ActionType
import boysband.githubservice.model.resourse.*
import boysband.githubservice.model.response.GithubEventResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GithubProcessing(
    private val utilsProcessing: UtilsProcessing,
    private val issueProcessing: IssueProcessing,
    private val commitProcessing: CommitProcessing,
    private val pullRequestProcessing: PullRequestProcessing,
    private val branchProcessing: BranchProcessing,
    private val githubActionsProcessing: GithubActionsProcessing,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getResponse(action: ActionDto): GithubEventResponse? {
        val chatId = action.user.idTgChat
        val token = action.token

        if (token.value.isBlank()) {
            logger.error("Token is empty for chatId: $chatId, actionId: ${action.id}")
            return null
        }

        if (token.user.idTgChat != chatId) {
            logger.error(
                "Token (id=${token.id}) does not belong to user (chatId=$chatId). " +
                "Token owner chatId: ${token.user.idTgChat}"
            )
            return null
        }

        val actionType = ActionType.fromMethodName(action.method.name)
        val link = action.query

        val parsedResource = utilsProcessing.parseGithubUrl(actionType, link)

        if (parsedResource == null) {
            logger.error("Failed to parse GitHub URL: $link for action type: $actionType")
            return null
        }

        return when (actionType) {
            ActionType.ISSUE -> {
                val issue = parsedResource as Issue
                logger.info("Processing Issue: ${issue.owner}/${issue.repo}#${issue.issueNumber}")

                val response = issueProcessing.processIssue(chatId, issue, token.value)

                if (response.hasNewEvents) {
                    logger.info("Found new events for Issue #${issue.issueNumber}: " +
                        "${response.newComments.size} new comments, " +
                        "${response.updatedComments.size} updated comments, " +
                        "${response.newEvents.size} new events, " +
                        "${response.updatedEvents.size} updated events")
                } else {
                    logger.debug("No new events for Issue #${issue.issueNumber}")
                }

                response
            }

            ActionType.COMMIT -> {
                val commit = parsedResource as Commit
                logger.info("Processing Commit: ${commit.owner}/${commit.repo}@${commit.ref.take(7)}")

                val response = commitProcessing.processCommit(chatId, commit, token.value)

                if (response.hasNewEvents) {
                    logger.info("Found new events for Commit ${commit.ref.take(7)}: " +
                        "${response.newComments.size} new comments, " +
                        "${response.updatedComments.size} updated comments")
                } else {
                    logger.debug("No new comments for Commit ${commit.ref.take(7)}")
                }

                response
            }

            ActionType.PULL_REQUEST -> {
                val pullRequest = parsedResource as PullRequest
                logger.info("Processing PR: ${pullRequest.owner}/${pullRequest.repo}#${pullRequest.prNumber}")

                val response = pullRequestProcessing.processPullRequest(chatId, pullRequest, token.value)

                if (response.hasNewEvents) {
                    logger.info("Found new events for PR #${pullRequest.prNumber}: " +
                        "${response.newComments.size} new comments, " +
                        "${response.updatedComments.size} updated comments, " +
                        "${response.newEvents.size} new events, " +
                        "${response.updatedEvents.size} updated events, " +
                        "${response.newCommits.size} new commits")
                } else {
                    logger.debug("No new events for PR #${pullRequest.prNumber}")
                }

                response
            }

            ActionType.BRANCH -> {
                val branch = parsedResource as Branch
                logger.info("Processing Branch: ${branch.owner}/${branch.repo}:${branch.name}")

                val response = branchProcessing.processBranch(chatId, branch, token.value)

                if (response.hasNewEvents) {
                    logger.info("Found ${response.newCommits.size} new commits for Branch ${branch.name}")
                } else {
                    logger.debug("No new commits for Branch ${branch.name}")
                }

                response
            }

            ActionType.GITHUB_ACTIONS -> {
                val githubActions = parsedResource as GithubActions
                logger.info("Processing GitHub Actions: ${githubActions.owner}/${githubActions.repo}" +
                    if (githubActions.workflowId.isNotEmpty()) "/${githubActions.workflowId}" else "")

                val response = githubActionsProcessing.processGithubActions(chatId, githubActions, token.value)

                if (response.hasNewEvents) {
                    logger.info("Found ${response.newRuns.size} new workflow runs, ${response.updatedRuns.size} updated runs")
                } else {
                    logger.debug("No new workflow runs")
                }

                response
            }
        }
    }
}

