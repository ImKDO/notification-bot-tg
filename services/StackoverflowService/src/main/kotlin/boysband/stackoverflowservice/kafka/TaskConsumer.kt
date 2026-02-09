package boysband.stackoverflowservice.kafka

import boysband.stackoverflowservice.dto.Task
import boysband.stackoverflowservice.service.TaskHandler
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class TaskConsumer(
    private val handler: TaskHandler
) {

    @KafkaListener(
        topics = ["stackoverflow"],
        groupId = "test-group-id"
    )
    suspend fun consumeTask(@Payload task: Task) {
        logger.info("Задание: $task")
        handler.handleTask(task)
    }


    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}