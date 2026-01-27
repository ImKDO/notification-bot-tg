package boysband.githubservice.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.DeserializationException
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConfig {

    private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)

    @Bean
    fun errorHandler(template: KafkaOperations<Any, Any>): DefaultErrorHandler {
        return DefaultErrorHandler(
            { record, exception ->

                val rootCause = exception.cause ?: exception

                when (rootCause) {
                    is DeserializationException -> {
                        val rawValue = rootCause.data
                            ?.let { String(it) }
                            ?: "null"

                        logger.error(
                            """
                        Kafka deserialization error:
                        topic=${record.topic()}
                        partition=${record.partition()}
                        offset=${record.offset()}
                        key=${record.key()}
                        value=$rawValue
                        reason=${rootCause.message}
                        """.trimIndent()
                        )
                    }

                    else -> {
                        logger.error(
                            "Kafka error: topic=${record.topic()}, offset=${record.offset()}, reason=${rootCause.message}"
                        )
                    }
                }
            },
            FixedBackOff(0L, 0)
        )
    }
}