package com.didit.application.admin.provided

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class AdminStatsResult(
    val totalUsers: Long,
    val newUsersToday: Long,
    val totalRetrospects: Long,
    val unansweredInquiries: Long,
    val dau: Long,
    val todayRetrospects: Long,
    val weeklyRetroTrend: List<DailyRetroCount>,
    val totalInputTokens: Long,
    val totalOutputTokens: Long,
    val textAnswerCount: Long,
    val voiceAnswerCount: Long,
    val recentUsers: List<RecentUserSummary>,
    val recentInquiries: List<RecentInquirySummary>,
)

data class DailyRetroCount(
    val date: LocalDate,
    val count: Long,
)

data class RecentUserSummary(
    val id: UUID,
    val email: String?,
    val nickname: String?,
    val job: String?,
    val createdAt: LocalDateTime?,
)

data class RecentInquirySummary(
    val id: UUID,
    val type: String,
    val content: String,
    val status: String,
    val createdAt: LocalDateTime?,
)
