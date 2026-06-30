package com.didit.application.admin.provided

import java.util.UUID

data class AdminLevelStat(
    val level: Int,
    val userCount: Long,
)

data class AdminMissionStat(
    val level: Int,
    val inProgress: Long,
    val completed: Long,
    val failed: Long,
    val total: Long,
    val completionRate: Double,
)

data class AdminBadgeStat(
    val badgeId: UUID,
    val name: String,
    val category: String,
    val conditionType: String,
    val active: Boolean,
    val acquiredCount: Long,
)
