package boysband.githubservice.kafka

import boysband.githubservice.model.dto.TokenValidationRequest
import boysband.githubservice.model.dto.TokenValidationResult
import boysband.githubservice.service.TokenValidationService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class TokenValidationConsumer(
    private val tokenValidationService: TokenValidationService,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {
    private val objectMapper = jacksonObjectMapper()

    @org.springframework.kafka.annotation.KafkaListener(
        topics = ["github_token_validate"],
        groupId = "github_token_validate_group",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    fun consumeTokenValidation(message: String) {
        logger.info("Received token validation request: $message")

        val request = try {
            objectMapper.readValue(message, TokenValidationRequest::class.java)
        } catch (e: Exception) {
            logger.error("Failed to parse token validation request: $message", e)
            return
        }

        val username = tokenValidationService.validateGithubToken(request.token)
        val valid = username != null

        val result = TokenValidationResult(
            telegramId = request.telegramId,
            token = request.token,
            valid = valid,
            username = username ?: "",
            service = request.service,
        )

        logger.info("Sending token validation result: valid=$valid, username=${result.username}")

        kafkaTemplate.send(TOPIC_RESULT, result.telegramId.toString(), result)
            .whenComplete { _, ex ->
                if (ex != null) {
                    logger.error("Failed to send token validation result", ex)
                } else {
                    logger.info("Token validation result sent for telegramId=${result.telegramId}")
                }
            }
    }

    companion object {
        private const val TOPIC_RESULT = "github_token_validate_result"
        private val logger = LoggerFactory.getLogger(TokenValidationConsumer::class.java)
    }
}
