package com.base.notificationservice.config

import com.base.notificationservice.event.EmailVerificationEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.converter.StringJsonMessageConverter
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper
import org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConsumerConfig(
    private val kafkaProperties: KafkaProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(KafkaConsumerConfig::class.java)

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        val props = kafkaProperties.buildConsumerProperties(null).toMutableMap<String, Any>()
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val typeMapper =
            DefaultJackson2JavaTypeMapper().apply {
                typePrecedence = Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID
                addTrustedPackages("*")
                idClassMapping =
                    mapOf(
                        "EMAIL_VERIFICATION" to EmailVerificationEvent::class.java,
                    )
            }

        val converter = StringJsonMessageConverter(objectMapper)
        converter.typeMapper = typeMapper

        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        factory.setRecordMessageConverter(converter)
        factory.setCommonErrorHandler(errorHandler())
        return factory
    }

    private fun errorHandler(): CommonErrorHandler =
        DefaultErrorHandler(
            { record, ex ->
                log.warn(
                    "Skipping invalid message on topic={}, partition={}, offset={}: {}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.value(),
                    ex,
                )
            },
            FixedBackOff(RETRY_INTERVAL_MS, MAX_ATTEMPTS),
        )

    companion object {
        private const val RETRY_INTERVAL_MS = 1000L
        private const val MAX_ATTEMPTS = 3L
    }
}
