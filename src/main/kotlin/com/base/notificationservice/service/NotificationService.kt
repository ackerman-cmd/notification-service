package com.base.notificationservice.service

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationChannel
import com.base.notificationservice.domain.NotificationStatus
import com.base.notificationservice.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val senders: List<NotificationSender>,
) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    private val sendersByChannel: Map<NotificationChannel, NotificationSender> by lazy {
        senders.associateBy { it.channel }
    }

    @Transactional
    fun send(notification: Notification) {
        notificationRepository.save(notification)
    }

    @Async("mailExecutor")
    @Transactional
    fun dispatch(notification: Notification) {
        dispatchSend(notification)
    }

    @Transactional
    fun retry(notification: Notification) {
        dispatchSend(notification)
    }

    private fun dispatchSend(notification: Notification) {
        val sender = sendersByChannel[notification.channel]
        if (sender == null) {
            log.error("No sender registered for channel: {}", notification.channel)
            markFailed(notification, "No sender for channel ${notification.channel}")
            return
        }

        sender
            .send(notification)
            .onSuccess {
                notification.status = NotificationStatus.SENT
                notification.sentAt = LocalDateTime.now()
                notificationRepository.save(notification)
                log.info(
                    "Notification SENT: id={}, type={}, channel={}",
                    notification.id,
                    notification.type,
                    notification.channel,
                )
            }.onFailure { ex ->
                markFailed(notification, ex.message)
            }
    }

    private fun markFailed(
        notification: Notification,
        errorMessage: String?,
    ) {
        notification.status = NotificationStatus.FAILED
        notification.error = errorMessage
        notification.retryCount++
        notificationRepository.save(notification)
        log.warn(
            "Notification FAILED: id={}, type={}, attempt={}, error={}",
            notification.id,
            notification.type,
            notification.retryCount,
            errorMessage,
        )
    }
}
