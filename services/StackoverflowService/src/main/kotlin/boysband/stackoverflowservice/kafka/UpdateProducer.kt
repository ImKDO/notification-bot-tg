package boysband.stackoverflowservice.kafka

import boysband.stackoverflowservice.dto.Update
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component

@Component
class UpdateProducer(
    private val sender: KafkaTemplate<String, Update>
) {

    fun sendUpdate(update: Update) {
        logger.info("Отправляем обновление: $update")
        try{
            val message = MessageBuilder
                .withPayload(update)
                .setHeader(KafkaHeaders.TOPIC, "updates")
                .build()

            sender.send(message).handle { result, ex ->
                when {
                    ex != null -> logger.error("Ошибка при отправке обновления: $update", ex)
                    else -> logger.info("Обновление успешно отправлено: $update")
                }
            }
        }
        catch(ex: Exception) {
            logger.error("Ошибка при отправке обновления: $update", ex)
        }
    }

    companion object{
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}