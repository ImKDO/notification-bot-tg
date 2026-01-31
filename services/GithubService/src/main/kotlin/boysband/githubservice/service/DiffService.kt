package boysband.githubservice.service

import boysband.githubservice.model.Issue
import org.slf4j.LoggerFactory

class DiffService(
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    val oldCommentIssue: MutableList<Issue> = emptyList<Issue>().toMutableList()

    fun diffIssue(newIssueList: List<Issue>): List<Issue> {
        if (oldCommentIssue.isEmpty()) {
            return newIssueList
        }
        val resultCommentIssue: MutableList<Issue> = emptyList<Issue>().toMutableList()
        oldCommentIssue.zip(newIssueList).forEach { (oldIssue, newIssue) ->
            if (oldIssue.updatedAt != newIssue.updatedAt) {
                resultCommentIssue.add(newIssue)
            }
        }
        oldCommentIssue.clear()
        oldCommentIssue.addAll(newIssueList)
        return resultCommentIssue
    }
}