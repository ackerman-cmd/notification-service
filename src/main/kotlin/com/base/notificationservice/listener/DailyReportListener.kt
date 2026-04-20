package com.base.notificationservice.listener

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationChannel
import com.base.notificationservice.domain.NotificationStatus
import com.base.notificationservice.domain.NotificationType
import com.base.notificationservice.event.DailyReportEvent
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
    topics = ["\${app.kafka.topics.daily-report}"],
    groupId = "\${spring.kafka.consumer.group-id}",
    containerFactory = "kafkaListenerContainerFactory",
)
class DailyReportListener(
    private val notificationService: NotificationService,
    private val notificationRepository: NotificationRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaHandler
    fun onDailyReport(event: DailyReportEvent) {
        log.debug("Received daily-report event: reportId={}, recipients={}", event.reportId, event.recipientEmails.size)

        event.recipientEmails.forEach { email ->
            val dedupKey = "DAILY_REPORT:${event.reportId}:$email"

            if (notificationRepository.existsByDeduplicationKey(dedupKey)) {
                log.info("Duplicate daily-report notification skipped: key={}", dedupKey)
                return@forEach
            }

            val notification =
                Notification(
                    id = UUID.randomUUID(),
                    userId = UUID(0, 0),
                    channel = NotificationChannel.EMAIL,
                    type = NotificationType.DAILY_REPORT,
                    recipient = email,
                    subject = event.subject,
                    status = NotificationStatus.PENDING,
                    deduplicationKey = dedupKey,
                    payload = objectMapper.writeValueAsString(event),
                )

            notificationService.send(notification)
            notificationService.dispatch(notification)

            log.info("Daily report notification dispatched: reportId={}, recipient={}", event.reportId, email)
        }
    }

    @KafkaHandler(isDefault = true)
    fun onUnknown(message: Any) {
        log.warn("Unknown message on daily-report topic, skipping: {}", message)
    }
}
