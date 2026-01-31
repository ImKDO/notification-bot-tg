package boysband.githubservice.service

import boysband.githubservice.model.utils.Comment
import boysband.githubservice.model.Issue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.client.body

@Component
class IssueProcessing(
    private val utilsProcessing: UtilsProcessing
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getTitle(issue: Issue, token: String): Issue? {
        val titleIssue =
            utilsProcessing.baseGithubRequestUrl(issue, "", token)
                ?.onStatus({ it.isError }) { request, response -> logger.error("Error get issue body: ${response.statusCode}") }
                ?.body<Issue>()
        return titleIssue
    }

     fun getComments(issue: Issue, token: String): List<Comment>? {
        val jsonComments =
            utilsProcessing.baseGithubRequestUrl(issue, "/comments", token)
                ?.onStatus({ it.isError }) { request, response -> logger.error("Error get issue body: ${response.statusCode}") }
                ?.body<List<Comment>>()
        return jsonComments
    }


}