package boysband.githubservice.service

import boysband.githubservice.model.Comment
import boysband.githubservice.model.Commit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.body

@Component
class CommitProcessing (
    private val utilsProcessing: UtilsProcessing
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getComments(commit: Commit, token: String): List<Comment>? {
        val commentsCommit =
            utilsProcessing.baseGithubRequestUrl(commit, "/comments", token)
                ?.onStatus({ it.isError }) { request, response -> logger.error("Error get commit body comments: ${response.statusCode}") }
                ?.body<List<Comment>>()
        return commentsCommit
    }

    fun getTitle(commit: Commit, token: String): Commit? {
        val titleCommit =
            utilsProcessing.baseGithubRequestUrl(commit, "", token)
                ?.onStatus({ it.isError }) { request, response -> logger.error("Error get commit body: ${response.statusCode}") }
                ?.body<Commit>()
        return titleCommit
    }
}