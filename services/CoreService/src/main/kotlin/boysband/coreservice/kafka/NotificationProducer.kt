package boysband.coreservice.kafka

import boysband.coreservice.dto.Notification
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

@Component
class NotificationProducer(
    private val sender: KafkaTemplate<String, Notification>
) {

    fun sendNotification(notification: Notification) {
        logger.info("Отправляем уведомление: $notification")
        try {
            val message = MessageBuilder
                .withPayload(notification)
                .setHeader(KafkaHeaders.TOPIC, "notifications")
                .build()

            sender.send(message).handle { result, ex ->
                when {
                    ex != null -> logger.error("Ошибка при отправке уведомления: $notification", ex)
                    else -> logger.info("Уведомление успешно отправлено: $notification")
                }
            }
        }
        catch (e: Exception) {
            logger.error("Ошибка при отправке уведомления: $notification", e)
        }
    }

    companion object{
        private val logger = LoggerFactory.getLogger(NotificationProducer::class.java)
    }
}