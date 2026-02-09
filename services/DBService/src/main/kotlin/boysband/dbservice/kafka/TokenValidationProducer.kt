package boysband.dbservice.kafka

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class TokenValidationProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    /**
     * Send token validation request to CoreService via Kafka.
     */
    fun sendTokenValidationRequest(request: TokenValidationRequestDto) {
        logger.info("Sending token validation request to Kafka: telegramId=${request.telegramId}, service=${request.service}")

        kafkaTemplate.send(TOPIC, request.telegramId.toString(), request)
            .whenComplete { _, ex ->
                if (ex != null) {
                    logger.error("Failed to send token validation request", ex)
                } else {
                    logger.info("Token validation request sent for telegramId=${request.telegramId}")
                }
            }
    }

    companion object {
        private const val TOPIC = "token_validation_request"
        private val logger = LoggerFactory.getLogger(TokenValidationProducer::class.java)
    }
}

data class TokenValidationRequestDto(
    val telegramId: Long = 0,
    val token: String = "",
    val service: String = "",
)
