package com.didit.application.retrospect.dto

import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveListItemResult(
    val id: UUID,
    val title: String?,
    val summary: String?,
    val completedAt: LocalDateTime?,
)
