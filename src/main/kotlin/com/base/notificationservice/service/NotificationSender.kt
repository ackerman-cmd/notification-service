package com.base.notificationservice.service

import com.base.notificationservice.domain.Notification
import com.base.notificationservice.domain.NotificationChannel

interface NotificationSender {
    val channel: NotificationChannel

    fun send(notification: Notification): Result<Unit>
}
