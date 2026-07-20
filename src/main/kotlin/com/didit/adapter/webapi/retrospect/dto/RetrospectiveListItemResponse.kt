package com.didit.adapter.webapi.retrospect.dto

import com.didit.application.retrospect.dto.RetrospectiveListItemResult
import com.didit.domain.retrospect.Retrospective
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveListItemResponse(
    val id: UUID,
    val title: String?,
    val summary: String?,
    val completedAt: LocalDateTime?,
) {
    companion object {
        fun from(retrospective: Retrospective): RetrospectiveListItemResponse =
            RetrospectiveListItemResponse(
                id = retrospective.id,
                title = retrospective.title,
                summary = retrospective.summary?.summary,
                completedAt = retrospective.completedAt,
            )

        fun from(result: RetrospectiveListItemResult): RetrospectiveListItemResponse =
            RetrospectiveListItemResponse(
                id = result.id,
                title = result.title,
                summary = result.summary,
                completedAt = result.completedAt,
            )
    }
}
