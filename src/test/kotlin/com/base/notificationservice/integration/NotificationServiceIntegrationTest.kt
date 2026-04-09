package com.base.notificationservice.integration

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationChannel
import com.base.notificationservice.domain.NotificationStatus
import com.base.notificationservice.domain.NotificationType
import com.base.notificationservice.repository.NotificationRepository
import com.base.notificationservice.service.NotificationService
import com.resend.services.emails.model.CreateEmailOptions
import com.resend.services.emails.model.CreateEmailResponse
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class NotificationServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var notificationService: NotificationService

    @Autowired
    lateinit var notificationRepository: NotificationRepository

    @Test
    fun `retry - saves notification with SENT status when Resend succeeds`() {
        val emailsService: com.resend.services.emails.Emails = mockk()
        every { resend.emails() } returns emailsService
        every { emailsService.send(any<CreateEmailOptions>()) } returns CreateEmailResponse()

        val notification = buildNotification()
        notificationService.send(notification)
        notificationService.retry(notification)

        val saved = notificationRepository.findById(notification.id!!)
        assertTrue(saved.isPresent)
        assertEquals(NotificationStatus.SENT, saved.get().status)
        assertNotNull(saved.get().sentAt)
    }

    @Test
    fun `retry - marks FAILED when Resend throws`() {
        val emailsService: com.resend.services.emails.Emails = mockk()
        every { resend.emails() } returns emailsService
        every { emailsService.send(any<CreateEmailOptions>()) } throws RuntimeException("Connection refused")

        val notification = buildNotification()
        notificationService.send(notification)
        notificationService.retry(notification)

        val saved = notificationRepository.findById(notification.id!!)
        assertTrue(saved.isPresent)
        assertEquals(NotificationStatus.FAILED, saved.get().status)
        assertEquals(1, saved.get().retryCount)
    }

    private fun buildNotification() =
        Notification(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            channel = NotificationChannel.EMAIL,
            type = NotificationType.EMAIL_VERIFICATION,
            recipient = "integration@example.com",
            subject = "Подтверждение email адреса",
            payload = """{"username":"john","verificationUrl":"http://localhost/verify?token=abc"}""",
        )
}
