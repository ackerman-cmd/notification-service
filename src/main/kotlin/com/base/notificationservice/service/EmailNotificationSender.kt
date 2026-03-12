package com.base.notificationservice.service

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationChannel
import com.base.notificationservice.domain.NotificationType
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Component
class EmailNotificationSender(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    @Value("\${app.mail.from}") private val mailFrom: String,
) : NotificationSender {
    override val channel = NotificationChannel.EMAIL

    private val log = LoggerFactory.getLogger(EmailNotificationSender::class.java)

    override fun send(notification: Notification): Result<Unit> =
        runCatching {
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(mailFrom)
            helper.setTo(notification.recipient)
            notification.subject?.let { helper.setSubject(it) }

            val htmlContent = buildHtmlContent(notification)
            helper.setText(htmlContent, true)

            mailSender.send(message)
            log.info(
                "Email sent: type={}, recipient={}, notificationId={}",
                notification.type,
                notification.recipient,
                notification.id,
            )
        }.onFailure { ex ->
            log.error(
                "Failed to send email: type={}, recipient={}, notificationId={}, error={}",
                notification.type,
                notification.recipient,
                notification.id,
                ex.message,
            )
        }

    private fun buildHtmlContent(notification: Notification): String {
        val templateName = resolveTemplateName(notification.type)
        val context = Context()
        context.setVariables(buildTemplateVariables(notification))
        return templateEngine.process(templateName, context)
    }

    private fun resolveTemplateName(type: NotificationType): String =
        when (type) {
            NotificationType.EMAIL_VERIFICATION -> "email/verification"
            NotificationType.PASSWORD_RESET -> "email/password-reset"
            NotificationType.ACCOUNT_BLOCKED -> "email/account-blocked"
            NotificationType.ACCOUNT_ACTIVATED -> "email/account-activated"
        }

    private fun buildTemplateVariables(notification: Notification): Map<String, Any?> {
        val base = mapOf("recipient" to notification.recipient)
        return when (notification.type) {
            NotificationType.EMAIL_VERIFICATION -> {
                val parts = notification.payload.let { parseEmailVerificationPayload(it) }
                base + parts
            }
            else -> base
        }
    }

    private fun parseEmailVerificationPayload(payload: String): Map<String, String> {
        val usernameMatch = Regex("\"username\"\\s*:\\s*\"([^\"]+)\"").find(payload)
        val urlMatch = Regex("\"verificationUrl\"\\s*:\\s*\"([^\"]+)\"").find(payload)
        return mapOf(
            "username" to (usernameMatch?.groupValues?.get(1) ?: ""),
            "verificationUrl" to (urlMatch?.groupValues?.get(1) ?: ""),
        )
    }
}
