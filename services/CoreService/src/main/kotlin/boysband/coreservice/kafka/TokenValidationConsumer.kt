package boysband.coreservice.kafka

import boysband.coreservice.dto.TokenValidationRequest
import boysband.coreservice.service.TokenValidationService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class TokenValidationConsumer(
    private val tokenValidationService: TokenValidationService,
) {
    private val objectMapper = jacksonObjectMapper()

    /**
     * Consume token validation request from DBService.
     * Forward to GithubService for actual validation.
     */
    @KafkaListener(
        id = "token-validation-consumer",
        topics = ["token_validation_request"],
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    fun consumeTokenValidationRequest(message: String) {
        logger.info("Received token validation request: $message")

        val request = try {
            objectMapper.readValue(message, TokenValidationRequest::class.java)
        } catch (e: Exception) {
            logger.error("Failed to parse token validation request: $message", e)
            return
        }

        when (request.service) {
            "github" -> tokenValidationService.forwardToGithubService(request)
            else -> logger.warn("Unknown service for token validation: ${request.service}")
        }
    }

    /**
     * Consume token validation result from GithubService.
     * Save to DB (if valid) and notify Bot.
     */
    @KafkaListener(
        id = "token-validation-result-consumer",
        topics = ["github_token_validate_result"],
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    suspend fun consumeTokenValidationResult(message: String) {
        logger.info("Received token validation result: $message")
        tokenValidationService.handleValidationResult(message)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TokenValidationConsumer::class.java)
    }
}
