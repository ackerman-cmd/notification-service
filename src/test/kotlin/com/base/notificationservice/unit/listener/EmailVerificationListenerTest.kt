package com.base.notificationservice.unit.listener

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationChannel
import com.base.notificationservice.domain.NotificationStatus
import com.base.notificationservice.domain.NotificationType
import com.base.notificationservice.event.EmailVerificationEvent
import com.base.notificationservice.listener.EmailVerificationListener
import com.base.notificationservice.repository.NotificationRepository
import com.base.notificationservice.service.NotificationService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class EmailVerificationListenerTest {
    private val notificationService: NotificationService = mockk(relaxed = true)
    private val notificationRepository: NotificationRepository = mockk()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private lateinit var listener: EmailVerificationListener

    @BeforeEach
    fun setUp() {
        listener = EmailVerificationListener(notificationService, notificationRepository, objectMapper)
    }

    @Test
    fun `onEmailVerification - creates notification and delegates to service`() {
        val event = buildEvent()
        every { notificationRepository.existsByDeduplicationKey(any()) } returns false

        listener.onEmailVerification(event)

        val slot = slot<Notification>()
        verify(exactly = 1) { notificationService.send(capture(slot)) }
        verify(exactly = 1) { notificationService.dispatch(any()) }

        val notification = slot.captured
        assertNotNull(notification.id)
        assertEquals(event.userId, notification.userId)
        assertEquals(event.email, notification.recipient)
        assertEquals(NotificationChannel.EMAIL, notification.channel)
        assertEquals(NotificationType.EMAIL_VERIFICATION, notification.type)
        assertEquals(NotificationStatus.PENDING, notification.status)
        assertEquals("Подтверждение email адреса", notification.subject)
    }

    @Test
    fun `onEmailVerification - skips duplicate event`() {
        val event = buildEvent()
        every { notificationRepository.existsByDeduplicationKey(any()) } returns true

        listener.onEmailVerification(event)

        verify(exactly = 0) { notificationService.send(any()) }
    }

    @Test
    fun `onUnknown - logs and does not process`() {
        listener.onUnknown("some unexpected payload")

        verify(exactly = 0) { notificationService.send(any()) }
    }

    private fun buildEvent() =
        EmailVerificationEvent(
            userId = UUID.randomUUID(),
            email = "user@example.com",
            username = "john",
            verificationToken = "token-123",
            verificationUrl = "http://localhost/verify?token=abc",
        )
}
