package com.base.notificationservice.service

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationChannel
import com.base.notificationservice.domain.NotificationStatus
import com.base.notificationservice.domain.NotificationType
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mail.javamail.JavaMailSender
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.IContext
import java.util.UUID

class EmailNotificationSenderTest {
    private val mailSender: JavaMailSender = mockk()
    private val templateEngine: TemplateEngine = mockk()
    private val mimeMessage: MimeMessage = mockk(relaxed = true)

    private val sender = EmailNotificationSender(mailSender, templateEngine, "noreply@test.com")

    @Test
    fun `send - returns success when mail sent`() {
        every { mailSender.createMimeMessage() } returns mimeMessage
        every { templateEngine.process(any<String>(), any<IContext>()) } returns "<html>test</html>"
        justRun { mailSender.send(mimeMessage) }

        val notification = buildNotification()
        val result = sender.send(notification)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { mailSender.send(mimeMessage) }
    }

    @Test
    fun `send - returns failure when mail throws`() {
        every { mailSender.createMimeMessage() } returns mimeMessage
        every { templateEngine.process(any<String>(), any<IContext>()) } returns "<html>test</html>"
        every { mailSender.send(mimeMessage) } throws RuntimeException("Connection refused")

        val result = sender.send(buildNotification())

        assertTrue(result.isFailure)
    }

    private fun buildNotification() =
        Notification(
            userId = UUID.randomUUID(),
            channel = NotificationChannel.EMAIL,
            type = NotificationType.EMAIL_VERIFICATION,
            recipient = "user@example.com",
            subject = "Verify email",
            status = NotificationStatus.PENDING,
            payload = """{"username":"john","verificationUrl":"http://localhost/verify?token=abc"}""",
        )
}
