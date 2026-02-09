package boysband.coreservice.kafka

import boysband.coreservice.dto.Task
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import kotlin.math.log

@Component
class TaskProducer(
    private val sender: KafkaTemplate<String, Task>
) {

    fun sendUpdate(task: Task, topic: String) {
        logger.info("Отправляем задание: $task")
        try{
            val message = MessageBuilder
                .withPayload(task)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build()

            sender.send(message).handle { result, ex ->
                when {
                    ex != null -> logger.error("Ошибка при отправке задания: $task", ex)
                    else -> logger.info("Задание успешно отправлено: $task")
                }
            }
        }
        catch(e: Exception) {
            logger.error("Ошибка при отправке задания: $task", e)
        }
    }

    companion object{
        private val logger = LoggerFactory.getLogger(TaskProducer::class.java)
    }

}