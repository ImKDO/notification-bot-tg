package boysband.stackoverflowservice.config

import boysband.stackoverflowservice.dto.Task
import boysband.stackoverflowservice.dto.Update
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaConfig(
    @param:Value($$"${kafka.bootstrap-servers}") private val bootstrapServers: String,
    @param:Value($$"${kafka.consumer.group-id}") private val groupId: String
) {

    private fun kafkaObjectMapper() = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Task> {
        val props = mutableMapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
        )

        val deserializer = JsonDeserializer(Task::class.java, kafkaObjectMapper()).apply {
            setRemoveTypeHeaders(true)
            addTrustedPackages("*")
            setUseTypeHeaders(false)
        }

        return DefaultKafkaConsumerFactory(props, StringDeserializer(), deserializer)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Task>
    ): ConcurrentKafkaListenerContainerFactory<String, Task> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Task>()
        factory.consumerFactory = consumerFactory
        return factory
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, Update> {
        val props = mutableMapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.LINGER_MS_CONFIG to 5
        )

        return DefaultKafkaProducerFactory(props, StringSerializer(), JsonSerializer(kafkaObjectMapper()))
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Update>): KafkaTemplate<String, Update> {
        return KafkaTemplate(producerFactory)
    }

}