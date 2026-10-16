package com.didit.application.admin.provided

import java.time.LocalDate

interface AdminAnalyticsFinder {
    fun find(
        from: LocalDate,
        to: LocalDate,
    ): AdminAnalyticsResult
}

data class AdminAnalyticsResult(
    val from: LocalDate,
    val to: LocalDate,
    val retrospectiveCount: Long,
    val completedCount: Long,
    val completionRate: Double,
    val retentionEligibleCount: Long,
    val retentionReturnedCount: Long,
    val retentionObservingCount: Long,
    val secondCompletionRateWithin14Days: Double,
    val returningUserCount: Long,
    val newUserCount: Long,
    val averageAnswerCount: Double?,
    val medianAnswerCount: Double?,
    val averageElapsedDurationSeconds: Double?,
    val medianElapsedDurationSeconds: Double?,
)
