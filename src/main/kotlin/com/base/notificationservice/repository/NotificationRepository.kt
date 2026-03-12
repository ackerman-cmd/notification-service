package com.base.notificationservice.repository

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun existsByDeduplicationKey(deduplicationKey: String): Boolean

    @Query(
        """
        SELECT n FROM Notification n
        WHERE n.status = :status
          AND n.retryCount < :maxRetries
          AND n.createdAt <= :before
        ORDER BY n.createdAt ASC
        """,
    )
    fun findRetryable(
        status: NotificationStatus,
        maxRetries: Int,
        before: LocalDateTime,
    ): List<Notification>
}
