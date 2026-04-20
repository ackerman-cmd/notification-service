package com.base.notificationservice.event

data class DailyReportEvent(
    val reportId: String,
    val subject: String,
    val recipientEmails: List<String>,
    val htmlBody: String,
    val s3Url: String?,
    val periodStart: String,
    val periodEnd: String,
    val totalAppeals: Int,
    val slaBreachesCount: Int,
)
