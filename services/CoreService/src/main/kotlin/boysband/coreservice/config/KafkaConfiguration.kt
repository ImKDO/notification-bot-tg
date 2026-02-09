package boysband.coreservice.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import boysband.coreservice.dto.Notification
import boysband.coreservice.dto.Task
import kotlin.Any
import kotlin.String

@Configuration
class KafkaConfiguration(
    @param:Value($$"${kafka.bootstrap-servers}") private val bootstrapServers: String,
    @param:Value($$"${kafka.consumer.group-id}") private val groupId: String
) {

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        val props = mutableMapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java
        )

        return DefaultKafkaConsumerFactory(
            props,
            StringDeserializer(),
            JsonDeserializer(Any::class.java).apply {
                setRemoveTypeHeaders(false)
                addTrustedPackages("*")
            }
        )
    }

    @Bean
    fun stringConsumerFactory(): ConsumerFactory<String, String> {
        val props = mutableMapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
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
    fun producerFactoryAny(): ProducerFactory<String, Any> {
        val props = mutableMapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.LINGER_MS_CONFIG to 5
        )

        return DefaultKafkaProducerFactory(props, StringSerializer(), JsonSerializer(jacksonObjectMapper()))
    }

    @Bean
    fun producerFactoryNotification(): ProducerFactory<String, Notification> {
        val props = mutableMapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.LINGER_MS_CONFIG to 5
        )

        return DefaultKafkaProducerFactory(props, StringSerializer(), JsonSerializer(jacksonObjectMapper()))
    }

    @Bean
    fun producerFactoryTask(): ProducerFactory<String, Task> {
        val props = mutableMapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.LINGER_MS_CONFIG to 5
        )

        return DefaultKafkaProducerFactory(props, StringSerializer(), JsonSerializer(jacksonObjectMapper()))
    }

    @Bean
    fun notificationKafkaTemplate(
        producerFactoryNotification: ProducerFactory<String, Notification>
    ): KafkaTemplate<String, Notification> = KafkaTemplate(producerFactoryNotification)

    @Bean
    fun taskKafkaTemplate(
        producerFactoryTask: ProducerFactory<String, Task>
    ): KafkaTemplate<String, Task> = KafkaTemplate(producerFactoryTask)


}