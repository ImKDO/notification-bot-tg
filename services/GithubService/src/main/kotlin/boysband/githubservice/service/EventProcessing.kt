package boysband.githubservice.service

import boysband.githubservice.model.utils.Event
import boysband.githubservice.model.Issue
import org.apache.el.parser.Token
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.body

@Component
class EventProcessing(
    private val utilsProcessing: UtilsProcessing
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getEvents(issue: Issue, token: String): List<Event>? {
        return utilsProcessing
            .baseGithubRequestUrl(issue, "/events" ,token)
            ?.onStatus({ it.isError }) { request, response -> logger.error("Error get issue events status: ${response.statusCode}") }
            ?.body<List<Event>>()
    }
}