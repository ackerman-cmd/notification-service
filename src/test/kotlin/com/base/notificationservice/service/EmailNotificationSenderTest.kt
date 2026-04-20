package com.base.notificationservice.service

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationChannel
import com.base.notificationservice.domain.NotificationStatus
import com.base.notificationservice.domain.NotificationType
import com.fasterxml.jackson.databind.ObjectMapper
import com.resend.Resend
import com.resend.core.exception.ResendException
import com.resend.services.emails.model.CreateEmailOptions
import com.resend.services.emails.model.CreateEmailResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.IContext
import java.util.UUID

class EmailNotificationSenderTest {
    private val resend: Resend = mockk()
    private val emailsService: com.resend.services.emails.Emails = mockk()
    private val templateEngine: TemplateEngine = mockk()
    private val objectMapper: ObjectMapper = mockk()

    private val sender = EmailNotificationSender(templateEngine, resend, objectMapper,"noreply@test.com")

    @Test
    fun `send - returns success when email sent`() {
        every { resend.emails() } returns emailsService
        every { emailsService.send(any<CreateEmailOptions>()) } returns CreateEmailResponse()
        every { templateEngine.process(any<String>(), any<IContext>()) } returns "<html>test</html>"

        val result = sender.send(buildNotification())

        assertTrue(result.isSuccess)
        verify(exactly = 1) { emailsService.send(any<CreateEmailOptions>()) }
    }

    @Test
    fun `send - returns failure when resend throws`() {
        every { resend.emails() } returns emailsService
        every { emailsService.send(any<CreateEmailOptions>()) } throws ResendException("API error")
        every { templateEngine.process(any<String>(), any<IContext>()) } returns "<html>test</html>"

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
