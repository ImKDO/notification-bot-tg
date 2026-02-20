package boysband.coreservice.kafka

import boysband.coreservice.client.DbServiceClient
import boysband.coreservice.client.MlServiceClient
import boysband.coreservice.dto.Notification
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class SummaryConsumer(
    private val mlServiceClient: MlServiceClient,
    private val notificationProducer: NotificationProducer,
    private val dbClient: WebClient,
) {
    private val objectMapper = jacksonObjectMapper()

    @KafkaListener(
        id = "summary-consumer",
        topics = ["summary_request"],
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    fun consumeSummaryRequest(message: String) {
        logger.info("Received summary request: $message")

        val node = try {
            objectMapper.readTree(message)
        } catch (e: Exception) {
            logger.error("Failed to parse summary request: $message", e)
            return
        }

        val telegramId = node.path("telegramId").asLong(0L)
        val notifications = node.path("notifications")
            ?.filter { it.isTextual }
            ?.map { it.asText() }
            ?: emptyList()

        if (telegramId == 0L) {
            logger.warn("Invalid summary request (missing telegramId): $message")
            return
        }

        if (notifications.isEmpty()) {
            notificationProducer.sendNotification(
                Notification(
                    chatId = telegramId,
                    title = "📊 Сводка уведомлений",
                    message = "У вас пока нет уведомлений для анализа.",
                    service = "ml",
                    type = "summary",
                    url = "",
                )
            )
            return
        }

        runBlocking {
            try {
                val summary = mlServiceClient.summarize(notifications)

                if (summary == null) {
                    notificationProducer.sendNotification(
                        Notification(
                            chatId = telegramId,
                            title = "📊 Сводка уведомлений",
                            message = "⚠️ ML-сервис временно недоступен. Попробуйте позже.",
                            service = "ml",
                            type = "summary",
                            url = "",
                        )
                    )
                    return@runBlocking
                }

                // Save summary to DB via DBService
                try {
                    dbClient.post()
                        .uri("/summary-reposts")
                        .bodyValue(
                            mapOf(
                                "user" to mapOf("idTgChat" to telegramId),
                                "report" to summary,
                            )
                        )
                        .retrieve()
                        .awaitBody<Map<String, Any>>()

                    logger.info("Summary saved to DB for user $telegramId")
                } catch (e: Exception) {
                    logger.error("Failed to save summary to DB for user $telegramId", e)
                }

                notificationProducer.sendNotification(
                    Notification(
                        chatId = telegramId,
                        title = "📊 Сводка уведомлений",
                        message = "$summary\n\n──────────────────\nНа основе ${notifications.size} последних уведомлений",
                        service = "ml",
                        type = "summary",
                        url = "",
                    )
                )

                logger.info("Summary sent to user $telegramId (${notifications.size} notifications)")

            } catch (e: Exception) {
                logger.error("Summary generation failed for user $telegramId", e)
                notificationProducer.sendNotification(
                    Notification(
                        chatId = telegramId,
                        title = "📊 Сводка уведомлений",
                        message = "⚠️ Ошибка при генерации сводки. Попробуйте позже.",
                        service = "ml",
                        type = "summary",
                        url = "",
                    )
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SummaryConsumer::class.java)
    }
}
