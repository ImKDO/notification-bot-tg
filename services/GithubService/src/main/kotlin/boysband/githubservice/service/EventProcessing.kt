package boysband.githubservice.service

import boysband.githubservice.model.utils.Event
import boysband.githubservice.model.Issue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.body

@Component
class EventProcessing(
    private val utilsProcessing: UtilsProcessing
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val incomingEvents: MutableList<Event> = emptyList<Event>().toMutableList()


    fun getEvents(issue: Issue, token: String): List<Event>? {
        return utilsProcessing
            .baseGithubRequestUrl(issue, "/events" ,token)
            ?.onStatus({ it.isError }) { request, response -> logger.error("Error get issue events status: ${response.statusCode}") }
            ?.body<List<Event>>()
    }
    fun getNewEvents(newIssueEvents: List<Event>?): List<Event>? {

        if (incomingEvents.isEmpty()) {
            incomingEvents.addAll(newIssueEvents!!)
            return incomingEvents
        }
        val newItemsOnly = newIssueEvents?.drop(incomingEvents.size)

        incomingEvents.clear()
        incomingEvents.addAll(newIssueEvents!!)

        return newItemsOnly
    }

}