package boysband.coreservice.kafka

import boysband.coreservice.service.UpdateHandlerService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class UpdateConsumer(
    private val handler: UpdateHandlerService,
) {

    @KafkaListener(
        id = "update-consumer",
        topics = [
            "updates",
            "github_issue_events",
            "github_commit_events",
            "github_pull_request_events",
            "github_branch_events",
            "github_actions_events",
        ],
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    suspend fun consumeUpdate(
        message: String,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
    ) {
        logger.info("Получено обновление из $topic: $message")
        handler.handleUpdate(topic, message)
    }

    companion object{
        private val logger = LoggerFactory.getLogger(UpdateConsumer::class.java)
    }
}