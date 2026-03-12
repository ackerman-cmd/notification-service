package com.base.notificationservice.listener

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationChannel
import com.base.notificationservice.domain.NotificationStatus
import com.base.notificationservice.domain.NotificationType
import com.base.notificationservice.event.EmailVerificationEvent
import com.base.notificationservice.repository.NotificationRepository
import com.base.notificationservice.service.NotificationService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@KafkaListener(
    topics = ["\${app.kafka.topics.email-verification}"],
    groupId = "\${spring.kafka.consumer.group-id}",
    containerFactory = "kafkaListenerContainerFactory",
)
class EmailVerificationListener(
    private val notificationService: NotificationService,
    private val notificationRepository: NotificationRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(EmailVerificationListener::class.java)

    @KafkaHandler
    fun onEmailVerification(event: EmailVerificationEvent) {
        log.debug("Received email-verification event: userId={}", event.userId)

        val deduplicationKey = buildDeduplicationKey(event)

        if (notificationRepository.existsByDeduplicationKey(deduplicationKey)) {
            log.info(
                "Duplicate event skipped: userId={}, type=EMAIL_VERIFICATION, key={}",
                event.userId,
                deduplicationKey,
            )
            return
        }

        val notification =
            Notification(
                id = UUID.randomUUID(),
                userId = event.userId,
                channel = NotificationChannel.EMAIL,
                type = NotificationType.EMAIL_VERIFICATION,
                recipient = event.email,
                subject = "Подтверждение email адреса",
                status = NotificationStatus.PENDING,
                deduplicationKey = deduplicationKey,
                payload = objectMapper.writeValueAsString(event),
            )

        notificationService.send(notification)
        notificationService.dispatch(notification)

        log.info(
            "Email verification notification processed: userId={}, notificationId={}",
            event.userId,
            notification.id,
        )
    }

    @KafkaHandler(isDefault = true)
    fun onUnknown(message: Any) {
        log.warn("Received unknown message on email-verification topic, skipping: {}", message)
    }

    private fun buildDeduplicationKey(event: EmailVerificationEvent): String =
        "EMAIL_VERIFICATION:${event.userId}:${event.verificationToken}"
}
