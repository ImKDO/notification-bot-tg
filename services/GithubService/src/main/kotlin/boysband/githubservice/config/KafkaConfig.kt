package boysband.githubservice.config

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.DeserializationException
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConfig(
    @param:Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
) {

    private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)

    @Bean
    fun stringConsumerFactory(): ConsumerFactory<String, String> {
        val props = mutableMapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        )
        return DefaultKafkaConsumerFactory(props, StringDeserializer(), StringDeserializer())
    }

    @Bean("stringKafkaListenerContainerFactory")
    fun stringKafkaListenerContainerFactory(
        stringConsumerFactory: ConsumerFactory<String, String>
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = stringConsumerFactory
        return factory
    }

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