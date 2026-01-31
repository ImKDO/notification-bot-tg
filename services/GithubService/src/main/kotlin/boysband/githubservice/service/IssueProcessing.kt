package boysband.githubservice.service

import boysband.githubservice.model.utils.Comment
import boysband.githubservice.model.Issue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.body

@Component
class IssueProcessing(
    private val utilsProcessing: UtilsProcessing
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val newCommentList = emptyList<Comment>().toMutableList()

    fun getTitle(issue: Issue, token: String): Issue? {
        return utilsProcessing.baseGithubRequestUrl(issue, "", token)
            ?.onStatus({ it.isError }) { request, response -> logger.error("Error get issue body: ${response.statusCode}") }
            ?.body<Issue>()
    }

    fun getComments(issue: Issue, token: String): List<Comment>? {
        return utilsProcessing.baseGithubRequestUrl(issue, "/comments", token)
            ?.onStatus({ it.isError }) { request, response -> logger.error("Error get issue body: ${response.statusCode}") }
            ?.body<List<Comment>>()
    }

    fun getNewOrUpdateComments(incomingCommentList: List<Comment>): List<Comment>? {
        if (newCommentList.isEmpty()){
            newCommentList.addAll(incomingCommentList)
            return incomingCommentList
        }

        val newCommentsOnly = incomingCommentList.drop(newCommentList.size).toMutableList()

        incomingCommentList.zip(newCommentList).forEach { (incoming, new) ->
            if (incoming.updatedAt != new.updatedAt) {
                newCommentsOnly.add(incoming)
            }
        }

        newCommentList.clear()
        newCommentList.addAll(incomingCommentList)

        return newCommentsOnly

    }

}