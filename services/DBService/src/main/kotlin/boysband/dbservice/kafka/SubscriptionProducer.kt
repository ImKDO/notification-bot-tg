package boysband.dbservice.kafka

import boysband.dbservice.entity.Action
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class SubscriptionProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    /**
     * Send newly created subscription (action) to CoreService for immediate processing.
     */
    fun sendSubscriptionRequest(action: Action) {
        val payload = mapOf(
            "actionId" to action.id,
            "telegramId" to (action.user?.idTgChat ?: 0),
            "serviceName" to (action.service?.name ?: ""),
            "methodName" to (action.method?.name ?: ""),
            "query" to action.query,
            "tokenValue" to (action.token?.value ?: ""),
            "tokenId" to (action.token?.id ?: 0),
            "userId" to (action.user?.id ?: 0),
            "serviceId" to (action.service?.id ?: 0),
            "methodId" to (action.method?.id ?: 0),
            "describe" to action.describe,
        )

        logger.info("Sending subscription request to Kafka: actionId=${action.id}")

        kafkaTemplate.send(TOPIC, action.id.toString(), payload)
            .whenComplete { _, ex ->
                if (ex != null) {
                    logger.error("Failed to send subscription request for actionId=${action.id}", ex)
                } else {
                    logger.info("Subscription request sent for actionId=${action.id}")
                }
            }
    }

    companion object {
        private const val TOPIC = "subscription_request"
        private val logger = LoggerFactory.getLogger(SubscriptionProducer::class.java)
    }
}
