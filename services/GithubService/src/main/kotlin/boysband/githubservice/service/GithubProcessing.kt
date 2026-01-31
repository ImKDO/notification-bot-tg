package boysband.githubservice.service

import boysband.githubservice.model.Commit
import boysband.githubservice.model.Issue
import boysband.githubservice.model.UserRequest
import boysband.githubservice.model.enums.ActionType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class GithubProcessing(
    private val baseUrlClient: RestClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val utilsProcessing: UtilsProcessing = UtilsProcessing(baseUrlClient)

    fun getResponse(userRequest: UserRequest): String {
        val token = userRequest.action.token
        val nameAction = userRequest.action.name
        val link = userRequest.link
        when (nameAction) {

            ActionType.ISSUE -> {
                val issue = utilsProcessing.parseGithubUrl(ActionType.ISSUE,link) as Issue
                val issueProcessing = IssueProcessing(utilsProcessing)
                val eventsProcessing = EventProcessing(utilsProcessing)
                val diffProcessing = DiffService()
                logger.info("Link parse: $issue")
                val titleIssue = issueProcessing.getTitle(issue, token)
                logger.info("Parsed title issue")
                val commentsIssue = issueProcessing.getComments(issue, token)
                logger.info("Parsed comments issue")
                val eventsList = eventsProcessing.getEvents(issue, token)
                logger.info("Parsed events list: $eventsList")
                val diffIssue = diffProcessing.diffIssue(issue)
            }

            ActionType.COMMIT -> {
                val commitProcessing = CommitProcessing(utilsProcessing)
                val commit = utilsProcessing.parseGithubUrl(ActionType.COMMIT,link) as Commit

                logger.info("Link parse: $commit")
                val titleCommit = commitProcessing.getTitle(commit, token)
                logger.info("Parsed title commit $titleCommit")
                val commentProcessing = commitProcessing.getComments(commit, token)
                logger.info("Parsed comments commit $commentProcessing")
            }


            ActionType.PULL_REQUEST -> {

            }

            else -> {

            }
        }
        return ""
    }
}

