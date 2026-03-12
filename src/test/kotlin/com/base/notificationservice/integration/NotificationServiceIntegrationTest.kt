package com.base.notificationservice.integration

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationChannel
import com.base.notificationservice.domain.NotificationStatus
import com.base.notificationservice.domain.NotificationType
import com.base.notificationservice.repository.NotificationRepository
import com.base.notificationservice.service.NotificationService
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
    fun `send - saves notification with SENT status and delivers email via GreenMail`() {
        val notification =
            Notification(
                id = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                channel = NotificationChannel.EMAIL,
                type = NotificationType.EMAIL_VERIFICATION,
                recipient = "integration@example.com",
                subject = "Подтверждение email адреса",
                payload = """{"username":"john","verificationUrl":"http://localhost/verify?token=abc"}""",
            )

        notificationService.send(notification)

        val saved = notificationRepository.findById(notification.id!!)
        assertTrue(saved.isPresent)
        assertEquals(NotificationStatus.SENT, saved.get().status)
        assertNotNull(saved.get().sentAt)

        val messages = greenMail.receivedMessages
        assertTrue(messages.isNotEmpty(), "Email should be delivered to GreenMail")
        assertEquals("integration@example.com", messages.last().allRecipients.first().toString())
    }

    @Test
    fun `send - marks FAILED when SMTP is unreachable`() {
        greenMail.stop()

        try {
            val notification =
                Notification(
                    id = UUID.randomUUID(),
                    userId = UUID.randomUUID(),
                    channel = NotificationChannel.EMAIL,
                    type = NotificationType.EMAIL_VERIFICATION,
                    recipient = "fail@example.com",
                    subject = "Should fail",
                    payload = """{"username":"x","verificationUrl":"http://localhost"}""",
                )

            notificationService.send(notification)

            val saved = notificationRepository.findById(notification.id!!)
            assertTrue(saved.isPresent)
            assertEquals(NotificationStatus.FAILED, saved.get().status)
            assertEquals(1, saved.get().retryCount)
        } finally {
            greenMail.start()
        }
    }
}
