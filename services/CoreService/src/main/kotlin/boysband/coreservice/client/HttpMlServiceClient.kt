package boysband.coreservice.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class HttpMlServiceClient(
    @Value("\${ml-service.base-url:http://localhost:8042}") private val baseUrl: String
) : MlServiceClient {

    private val client = WebClient.builder()
        .baseUrl(baseUrl)
        .build()

    override suspend fun summarize(notifications: List<String>, maxTokens: Int): String? {
        return try {
            val request = mapOf(
                "notifications" to notifications,
                "max_tokens" to maxTokens
            )

            val response = client.post()
                .uri("/summarize")
                .bodyValue(request)
                .retrieve()
                .awaitBody<Map<String, Any>>()

            response["summary"] as? String
        } catch (e: Exception) {
            logger.error("Failed to call MLService: ${e.message}", e)
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HttpMlServiceClient::class.java)
    }
}
