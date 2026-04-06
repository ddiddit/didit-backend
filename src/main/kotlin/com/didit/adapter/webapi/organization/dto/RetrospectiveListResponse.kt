package com.didit.adapter.webapi.organization.dto

import com.didit.domain.retrospect.Retrospective
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveListResponse(
    val id: UUID,
    val title: String?,
    val summary: String?,
    val completedAt: LocalDateTime?,
) {
    companion object {
        fun from(retrospective: Retrospective): RetrospectiveListResponse =
            RetrospectiveListResponse(
                id = retrospective.id,
                title = retrospective.title,
                summary = retrospective.summary?.summary,
                completedAt = retrospective.completedAt,
            )
    }
}
