package com.didit.application.admin.provided

import java.time.LocalDateTime
import java.util.UUID

data class AdminBadgeResult(
    val id: UUID,
    val name: String,
    val description: String,
    val conditionType: String,
    val acquiredCount: Long,
    val createdAt: LocalDateTime?,
)

data class AdminBadgeHolder(
    val userId: UUID,
    val email: String?,
    val nickname: String?,
    val acquiredAt: LocalDateTime,
)
