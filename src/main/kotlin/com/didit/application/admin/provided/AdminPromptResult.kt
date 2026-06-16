package com.didit.application.admin.provided

import java.time.LocalDateTime
import java.util.UUID

data class AdminPromptResult(
    val id: UUID,
    val jobType: String,
    val promptType: String,
    val content: String,
    val updatedAt: LocalDateTime,
    val updatedBy: String?,
)
