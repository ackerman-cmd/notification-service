package com.base.notificationservice.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "notifications", schema = "notification_service")
class Notification(
    @Id
    val id: UUID? = null,
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    val channel: NotificationChannel,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    val type: NotificationType,
    @Column(name = "recipient", nullable = false)
    val recipient: String,
    @Column(name = "subject")
    val subject: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: NotificationStatus = NotificationStatus.PENDING,
    @Column(name = "error", columnDefinition = "TEXT")
    var error: String? = null,
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,
    @Column(name = "deduplication_key")
    val deduplicationKey: String? = null,
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null,
)
