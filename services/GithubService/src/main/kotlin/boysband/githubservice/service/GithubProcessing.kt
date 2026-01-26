package boysband.githubservice.service

import boysband.githubservice.model.Issue
import boysband.githubservice.model.UserRequest
import boysband.githubservice.model.enums.ActionType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class GithubProcessing(
    private val baseUrlClient: RestClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun getResponse(userRequest: UserRequest): String {
        val token = userRequest.action.token
        val nameAction = userRequest.action.name

        when (nameAction) {
            ActionType.COMMIT -> {

            }

            ActionType.ISSUE -> {
                val owner = userRequest.dataForRequest.get("owner")
                val repo = userRequest.dataForRequest.get("repo")
                val id = userRequest.dataForRequest.get("id")

                baseUrlClient.get()
                    .uri("/repos/$owner/$repo/issues/$id")
                    .header("Authorization", "Bearer $token")
                    .retrieve().onStatus({it.isError}) { req, res ->
                        logger.error("Ошибочка!: ${res.statusCode}")
                    }
            }

            ActionType.PULL_REQUEST -> {

            }

            else -> {

            }
        }
        return TODO("Provide the return value")
    }
}