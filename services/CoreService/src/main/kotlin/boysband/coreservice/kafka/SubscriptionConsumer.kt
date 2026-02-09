package boysband.coreservice.kafka

import boysband.coreservice.dto.*
import boysband.coreservice.dto.Notification
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Consumes subscription_request from DBService when a new action is created.
 * Immediately forwards the action to GithubService for processing,
 * so the user gets first results right away (without waiting for the scheduler).
 */
@Component
class SubscriptionConsumer(
    private val taskProducer: TaskProducer,
    private val notificationProducer: NotificationProducer,
) {
    private val objectMapper = jacksonObjectMapper()

    @KafkaListener(
        id = "subscription-consumer",
        topics = ["subscription_request"],
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    fun consumeSubscription(message: String) {
        logger.info("Received subscription request: $message")

        val node = try {
            objectMapper.readTree(message)
        } catch (e: Exception) {
            logger.error("Failed to parse subscription request: $message", e)
            return
        }

        val actionId = node.path("actionId").asInt(0)
        val telegramId = node.path("telegramId").asLong(0L)
        val serviceName = node.path("serviceName").asText("")
        val methodName = node.path("methodName").asText("")
        val query = node.path("query").asText("")
        val tokenValue = node.path("tokenValue").asText("")
        val tokenId = node.path("tokenId").asInt(0)
        val userId = node.path("userId").asInt(0)
        val serviceId = node.path("serviceId").asInt(0)
        val methodId = node.path("methodId").asInt(0)
        val describe = node.path("describe").asText("")

        if (actionId == 0 || telegramId == 0L || query.isBlank()) {
            logger.warn("Invalid subscription request (missing required fields): $message")
            return
        }

        // Route to appropriate service
        when (serviceName.lowercase()) {
            "github" -> {
                val task = boysband.coreservice.dto.Task.GithubTask(
                    id = actionId,
                    method = MethodDto(id = methodId, name = methodName),
                    token = TokenDto(id = tokenId, value = tokenValue, user = UserDto(id = userId, idTgChat = telegramId)),
                    user = UserDto(id = userId, idTgChat = telegramId),
                    service = ServiceDto(id = serviceId, link = "https://api.github.com", name = serviceName),
                    describe = describe,
                    query = query,
                )

                logger.info("Forwarding subscription to github_request: actionId=$actionId, query=$query")
                taskProducer.sendUpdate(task, "github_request")
            }
            else -> {
                logger.warn("Unknown service for subscription: $serviceName")
            }
        }

        // Send confirmation notification to the bot
        notificationProducer.sendNotification(
            Notification(
                chatId = telegramId,
                title = "Подписка оформлена ✅",
                message = "Тип: $methodName\nРесурс: $query\n\nДанные будут обновляться каждые 10 секунд.",
                service = serviceName.lowercase(),
                type = methodName.lowercase(),
                url = query,
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SubscriptionConsumer::class.java)
    }
}
