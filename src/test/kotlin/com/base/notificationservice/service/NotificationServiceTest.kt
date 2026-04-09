package com.base.notificationservice.service

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationChannel
import com.base.notificationservice.domain.NotificationStatus
import com.base.notificationservice.domain.NotificationType
import com.base.notificationservice.repository.NotificationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class NotificationServiceTest {
    private val notificationRepository: NotificationRepository = mockk()
    private val emailSender: EmailNotificationSender = mockk()

    private lateinit var service: NotificationService

    @BeforeEach
    fun setUp() {
        every { emailSender.channel } returns NotificationChannel.EMAIL
        service = NotificationService(notificationRepository, listOf(emailSender))
    }

    @Test
    fun `send - saves notification with PENDING status`() {
        val notification = buildNotification()
        every { notificationRepository.save(any()) } answers { firstArg() }

        service.send(notification)

        verify(exactly = 1) { notificationRepository.save(any()) }
        assertEquals(NotificationStatus.PENDING, notification.status)
    }

    @Test
    fun `retry - marks SENT on success`() {
        val notification = buildNotification()
        every { notificationRepository.save(any()) } answers { firstArg() }
        every { emailSender.send(any()) } returns Result.success(Unit)

        service.retry(notification)

        verify(exactly = 1) { notificationRepository.save(any()) }
        assertEquals(NotificationStatus.SENT, notification.status)
    }

    @Test
    fun `retry - marks FAILED on sender error`() {
        val notification = buildNotification()
        every { notificationRepository.save(any()) } answers { firstArg() }
        every { emailSender.send(any()) } returns Result.failure(RuntimeException("API error"))

        service.retry(notification)

        assertEquals(NotificationStatus.FAILED, notification.status)
        assertEquals(1, notification.retryCount)
    }

    @Test
    fun `retry - marks FAILED when no sender registered for channel`() {
        every { emailSender.channel } returns NotificationChannel.SMS
        service = NotificationService(notificationRepository, listOf(emailSender))

        val notification = buildNotification()
        every { notificationRepository.save(any()) } answers { firstArg() }

        service.retry(notification)

        assertEquals(NotificationStatus.FAILED, notification.status)
    }

    private fun buildNotification() =
        Notification(
            userId = UUID.randomUUID(),
            channel = NotificationChannel.EMAIL,
            type = NotificationType.EMAIL_VERIFICATION,
            recipient = "test@example.com",
            subject = "Verify your email",
            payload = "{}",
        )
}
