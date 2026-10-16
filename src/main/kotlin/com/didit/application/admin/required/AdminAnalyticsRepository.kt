package com.didit.application.admin.required

import java.time.LocalDateTime

interface AdminAnalyticsRepository {
    fun load(
        from: LocalDateTime,
        to: LocalDateTime,
        launchAt: LocalDateTime,
        now: LocalDateTime,
    ): AdminAnalyticsSnapshot
}

data class AdminAnalyticsSnapshot(
    val retrospectives: List<AnalyticsRetrospective>,
    val newUserCount: Long,
    val returningUserCount: Long,
    val retentionEligibleCount: Long,
    val retentionReturnedCount: Long,
    val retentionObservingCount: Long,
)

data class AnalyticsRetrospective(
    val completed: Boolean,
    val answerCount: Int,
    val elapsedDurationSeconds: Long?,
)
