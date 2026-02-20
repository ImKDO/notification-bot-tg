package boysband.dbservice.kafka

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class SummaryProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    fun sendSummaryRequest(telegramId: Long, notifications: List<String>) {
        val payload = mapOf(
            "telegramId" to telegramId,
            "notifications" to notifications,
        )

        logger.info("Sending summary request to Kafka: telegramId=$telegramId, count=${notifications.size}")

        kafkaTemplate.send(TOPIC, telegramId.toString(), payload)
            .whenComplete { _, ex ->
                if (ex != null) {
                    logger.error("Failed to send summary request for telegramId=$telegramId", ex)
                } else {
                    logger.info("Summary request sent for telegramId=$telegramId")
                }
            }
    }

    companion object {
        private const val TOPIC = "summary_request"
        private val logger = LoggerFactory.getLogger(SummaryProducer::class.java)
    }
}
