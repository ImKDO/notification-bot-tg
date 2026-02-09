package boysband.coreservice.service

import boysband.coreservice.dto.Notification
import boysband.coreservice.dto.TokenValidationRequest
import boysband.coreservice.dto.TokenValidationResult
import boysband.coreservice.kafka.NotificationProducer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class TokenValidationService(
    private val anyKafkaTemplate: KafkaTemplate<String, Any>,
    private val notificationProducer: NotificationProducer,
    private val webClient: WebClient,
) {
    private val objectMapper = jacksonObjectMapper()

    fun forwardToGithubService(request: TokenValidationRequest) {
        logger.info("Forwarding token validation to GithubService for telegramId=${request.telegramId}")

        val message = MessageBuilder
            .withPayload(request as Any)
            .setHeader(KafkaHeaders.TOPIC, TOPIC_GITHUB_TOKEN_VALIDATE)
            .build()

        anyKafkaTemplate.send(message).handle { _, ex ->
            if (ex != null) {
                logger.error("Failed to send token validation request to GithubService", ex)
            } else {
                logger.info("Token validation request sent to GithubService for telegramId=${request.telegramId}")
            }
        }
    }

    suspend fun handleValidationResult(payload: String) {
        val result = try {
            objectMapper.readValue(payload, TokenValidationResult::class.java)
        } catch (e: Exception) {
            logger.error("Failed to parse token validation result: $payload", e)
            return
        }

        logger.info(
            "Token validation result: telegramId=${result.telegramId}, " +
                "valid=${result.valid}, username=${result.username}"
        )

        if (result.valid) {
            try {
                saveTokenToDb(result)
                logger.info("Token saved to DB for telegramId=${result.telegramId}")
            } catch (e: Exception) {
                logger.error("Failed to save token to DBService for telegramId=${result.telegramId}", e)
                sendAuthNotification(result.telegramId, false, "", result.service)
                return
            }
        }

        sendAuthNotification(result.telegramId, result.valid, result.username, result.service)
    }

    private suspend fun saveTokenToDb(result: TokenValidationResult) {
        val tokenBody = mapOf(
            "value" to result.token,
            "user" to mapOf("idTgChat" to result.telegramId),
        )

        webClient.post()
            .uri("/tokens")
            .bodyValue(tokenBody)
            .retrieve()
            .toBodilessEntity()
            .awaitSingle()
    }

    private fun sendAuthNotification(telegramId: Long, valid: Boolean, username: String, service: String) {
        val (title, message) = if (valid) {
            "Авторизация ${service.capitalize()}" to
                "Токен успешно авторизован ✅\nПользователь: $username"
        } else {
            "Авторизация ${service.capitalize()}" to
                "Токен не прошёл валидацию ❌\nПроверьте правильность токена и попробуйте снова."
        }

        val notification = Notification(
            chatId = telegramId,
            title = title,
            message = message,
            service = service,
            type = "auth",
            url = "",
        )
        notificationProducer.sendNotification(notification)
    }

    companion object {
        private const val TOPIC_GITHUB_TOKEN_VALIDATE = "github_token_validate"
        private val logger = LoggerFactory.getLogger(TokenValidationService::class.java)
    }
}
