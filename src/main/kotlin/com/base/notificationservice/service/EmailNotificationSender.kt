package com.base.notificationservice.service

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationChannel
import com.base.notificationservice.domain.NotificationType
import com.base.notificationservice.event.DailyReportEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Component
class EmailNotificationSender(
    private val templateEngine: TemplateEngine,
    private val resend: Resend,
    private val objectMapper: ObjectMapper,
    @Value("\${app.mail.from}") private val mailFrom: String,
) : NotificationSender {
    override val channel = NotificationChannel.EMAIL

    private val log = LoggerFactory.getLogger(EmailNotificationSender::class.java)

    override fun send(notification: Notification): Result<Unit> =
        runCatching {
            val htmlContent =
                if (notification.type == NotificationType.DAILY_REPORT) {
                    extractHtmlBodyFromPayload(notification.payload)
                } else {
                    buildHtmlContent(notification)
                }

            val params =
                CreateEmailOptions
                    .builder()
                    .from(mailFrom)
                    .to(notification.recipient)
                    .subject(notification.subject ?: "No Subject")
                    .html(htmlContent)
                    .build()

            resend.emails().send(params)

            log.info(
                "Email sent: type={}, recipient={}, id={}",
                notification.type,
                notification.recipient,
                notification.id,
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
            NotificationType.DAILY_REPORT -> error("DAILY_REPORT uses raw htmlBody, not a template")
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

    private fun extractHtmlBodyFromPayload(payload: String): String = objectMapper.readValue(payload, DailyReportEvent::class.java).htmlBody
}
