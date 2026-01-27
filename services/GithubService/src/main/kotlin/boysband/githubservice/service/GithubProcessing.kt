package boysband.githubservice.service

import boysband.githubservice.model.Author
import boysband.githubservice.model.Branch
import boysband.githubservice.model.Comment
import boysband.githubservice.model.Commit
import boysband.githubservice.model.GithubActions
import boysband.githubservice.model.Issue
import boysband.githubservice.model.PullRequest
import boysband.githubservice.model.UserRequest
import boysband.githubservice.model.enums.ActionType
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Service
class GithubProcessing(
    private val baseUrlClient: RestClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getResponse(userRequest: UserRequest): String {
        val token = userRequest.action.token
        val nameAction = userRequest.action.name
        val link = userRequest.link
        when (nameAction) {

            ActionType.ISSUE -> {
                val issue: Issue = parseGithubUrl(link)
                logger.info("Link parse: ${issue}")

                val titleIssue = getTitleIssue(issue, token)
//                val commentsIssue = getIssueComments(issue, token)

                logger.info("Issue parse: ${titleIssue}")

//                logger.info("Issue body parse: ${responseBody.toString()}")

            }

            ActionType.COMMIT -> {

            }


            ActionType.PULL_REQUEST -> {

            }

            else -> {

            }
        }
        return ""
    }

    private fun getTitleIssue(issue: Issue, token: String): Issue? {
        val jsonNode =
            baseGithubIssueUrl(issue, "", token)
                ?.onStatus({ it.isError }) { request, response -> logger.error("Error get issue body: ${response.statusCode}") }
                ?.body<Issue>()
        return jsonNode
    }

    private fun getIssueComments(issue: Issue, token: String): List<Comment>? {
        val jsonComments =
            baseGithubIssueUrl(issue, "comments", token)
            ?.onStatus({ it.isError }) { request, response -> logger.error("Error get issue body: ${response.statusCode}") }
            ?.body<List<Comment>>()
        return jsonComments
    }

    private fun parseGithubUrl(url: String): Issue {
        val regex = Regex("""github\.com/([^/]+)/([^/]+)/issues/(\d+)""")
        val matchResult = regex.find(url)

        if (matchResult != null) {
            val (owner, repo, issueNumber) = matchResult.destructured

            return Issue(
                issueNumber = issueNumber.toInt(),
                owner = owner,
                repo = repo
            )
        } else {
            return Issue(
                issueNumber = 0,
                owner = "",
                repo = ""
            )
        }
    }

    private fun parseGithubIssueAuthor(issue: Issue, token: String, endpoint: String): Author {
        val jsonData = baseGithubIssueUrl(issue, endpoint, token)?.body<JsonNode>()
        return Author(
            name = jsonData?.get("user")?.get("login") as String,
            linkOnAuthor = jsonData.get("user")?.get("html_url") as String,
        )
    }

    private fun baseGithubIssueUrl(
        objectForRequest: Any,
        endpoint: String,
        token: String,
    ): RestClient.ResponseSpec? {

        when (objectForRequest) {
            is Issue -> {
                val issueNumber = objectForRequest.issueNumber
                val owner = objectForRequest.owner
                val repo = objectForRequest.repo

                return baseUrlClient.get()
                    .uri("/repos/$owner/$repo/issues/$issueNumber$endpoint")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
            }

            is Commit -> {

            }

            is PullRequest -> {

            }

            is GithubActions -> {

            }

            is Branch -> {

            }

            else -> {

            }
        }
        return null
    }
}

