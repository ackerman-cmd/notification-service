package com.base.notificationservice.scheduler

import com.base.notificationservice.domain.NotificationStatus
import com.base.notificationservice.repository.NotificationRepository
import com.base.notificationservice.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class NotificationRetryScheduler(
    private val notificationRepository: NotificationRepository,
    private val notificationService: NotificationService,
    @Value("\${app.notification.retry.max-retries:5}") private val maxRetries: Int,
) {
    private val log = LoggerFactory.getLogger(NotificationRetryScheduler::class.java)

    @Scheduled(fixedDelayString = "\${app.notification.retry.scheduler-interval-ms:60000}")
    @Transactional
    fun retryFailed() {
        val cutoff = LocalDateTime.now()

        val candidates =
            notificationRepository.findRetryable(
                status = NotificationStatus.FAILED,
                maxRetries = maxRetries,
                before = cutoff,
            )

        if (candidates.isEmpty()) return

        log.info("Retrying {} failed notifications", candidates.size)

        candidates.forEach { notification ->
            val backoffMinutes = exponentialBackoffMinutes(notification.retryCount)
            val earliestRetry = notification.createdAt.plusMinutes(backoffMinutes)

            if (LocalDateTime.now().isBefore(earliestRetry)) {
                log.debug(
                    "Skipping notification {} – too soon (next retry at {})",
                    notification.id,
                    earliestRetry,
                )
                return@forEach
            }

            if (notification.retryCount >= maxRetries) {
                notification.status = NotificationStatus.PERMANENTLY_FAILED
                notificationRepository.save(notification)
                log.error(
                    "Notification {} permanently failed after {} retries",
                    notification.id,
                    notification.retryCount,
                )
                return@forEach
            }

            log.info(
                "Retrying notification: id={}, type={}, attempt={}",
                notification.id,
                notification.type,
                notification.retryCount + 1,
            )
            notificationService.retry(notification)
        }
    }

    private fun exponentialBackoffMinutes(retryCount: Int): Long {
        val base = 1L
        return base * (1L shl retryCount.coerceAtMost(MAX_BACKOFF_SHIFT))
    }

    companion object {
        private const val MAX_BACKOFF_SHIFT = 5
    }
}
