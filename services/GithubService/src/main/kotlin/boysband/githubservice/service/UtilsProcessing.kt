package boysband.githubservice.service

import boysband.githubservice.model.resourse.Branch
import boysband.githubservice.model.resourse.Commit
import boysband.githubservice.model.resourse.GithubActions
import boysband.githubservice.model.resourse.Issue
import boysband.githubservice.model.resourse.PullRequest
import boysband.githubservice.model.enums.ActionType
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class UtilsProcessing(
    private val baseUrlClient: RestClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun baseGithubRequestUrl(
        objectForRequest: Any,
        endpoint: String,
        token: String,
    ): RestClient.ResponseSpec? {

        return when (objectForRequest) {
            is Issue -> {
                val issueNumber = objectForRequest.issueNumber
                val owner = objectForRequest.owner
                val repo = objectForRequest.repo

                baseUrlClient.get()
                    .uri("/repos/$owner/$repo/issues/$issueNumber$endpoint")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
            }

            is Commit -> {
                val owner = objectForRequest.owner
                val repo = objectForRequest.repo
                val ref = objectForRequest.ref

                baseUrlClient.get()
                    .uri("/repos/$owner/$repo/commits/$ref$endpoint")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
            }

            is PullRequest -> {
                val owner = objectForRequest.owner
                val repo = objectForRequest.repo
                val prNumber = objectForRequest.prNumber

                baseUrlClient.get()
                    .uri("/repos/$owner/$repo/pulls/$prNumber$endpoint")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
            }

            is GithubActions -> {
                val owner = objectForRequest.owner
                val repo = objectForRequest.repo
                val workflowId = objectForRequest.workflowId

                val uri = if (workflowId.isNotEmpty()) {
                    "/repos/$owner/$repo/actions/workflows/$workflowId/runs$endpoint"
                } else {
                    "/repos/$owner/$repo/actions/runs$endpoint"
                }

                baseUrlClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
            }

            is Branch -> {
                val owner = objectForRequest.owner
                val repo = objectForRequest.repo
                val branch = objectForRequest.name

                baseUrlClient.get()
                    .uri("/repos/$owner/$repo/commits?sha=$branch$endpoint")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
            }

            else -> {
                logger.error("Unsupported resource type: ${objectForRequest::class.simpleName}")
                null
            }
        }
    }

    fun parseGithubUrl(typeAction: ActionType, url: String): Any? {
        return when (typeAction) {
            ActionType.ISSUE -> parseIssueUrl(url)
            ActionType.COMMIT -> parseCommitUrl(url)
            ActionType.PULL_REQUEST -> parsePullRequestUrl(url)
            ActionType.BRANCH -> parseBranchUrl(url)
            ActionType.GITHUB_ACTIONS -> parseGithubActionsUrl(url)
        }
    }

    private fun parseIssueUrl(url: String): Issue? {
        val regex = Regex("""github\.com/([^/]+)/([^/]+)/issues/(\d+)""")
        val matchResult = regex.find(url) ?: return null

        val (owner, repo, issueNumber) = matchResult.destructured
        return Issue(
            owner = owner,
            repo = repo,
            issueNumber = issueNumber.toInt()
        )
    }

    private fun parseCommitUrl(url: String): Commit? {
        val regex = Regex("""github\.com/([^/]+)/([^/]+)/commit/([0-9a-fA-F]+)""")
        val matchResult = regex.find(url) ?: return null

        val (owner, repo, ref) = matchResult.destructured
        return Commit(
            owner = owner,
            repo = repo,
            ref = ref
        )
    }

    private fun parsePullRequestUrl(url: String): PullRequest? {
        val regex = Regex("""github\.com/([^/]+)/([^/]+)/pull/(\d+)""")
        val matchResult = regex.find(url) ?: return null

        val (owner, repo, prNumber) = matchResult.destructured
        return PullRequest(
            owner = owner,
            repo = repo,
            prNumber = prNumber.toInt()
        )
    }

    private fun parseBranchUrl(url: String): Branch? {
        // Формат: github.com/owner/repo/tree/branch-name
        val regex = Regex("""github\.com/([^/]+)/([^/]+)/tree/(.+)""")
        val matchResult = regex.find(url) ?: return null

        val (owner, repo, branchName) = matchResult.destructured
        return Branch(
            owner = owner,
            repo = repo,
            name = branchName
        )
    }

    private fun parseGithubActionsUrl(url: String): GithubActions? {
        val workflowRegex = Regex("""github\.com/([^/]+)/([^/]+)/actions/workflows/([^/]+)""")
        val workflowMatch = workflowRegex.find(url)

        if (workflowMatch != null) {
            val (owner, repo, workflowId) = workflowMatch.destructured
            return GithubActions(
                owner = owner,
                repo = repo,
                workflowId = workflowId
            )
        }

        val actionsRegex = Regex("""github\.com/([^/]+)/([^/]+)/actions""")
        val actionsMatch = actionsRegex.find(url) ?: return null

        val (owner, repo) = actionsMatch.destructured
        return GithubActions(
            owner = owner,
            repo = repo
        )
    }
}