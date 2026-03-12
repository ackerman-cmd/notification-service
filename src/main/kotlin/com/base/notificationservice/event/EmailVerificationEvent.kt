package com.base.notificationservice.event

import java.util.UUID

data class EmailVerificationEvent(
    val userId: UUID,
    val email: String,
    val username: String,
    val verificationToken: String,
    val verificationUrl: String,
)
