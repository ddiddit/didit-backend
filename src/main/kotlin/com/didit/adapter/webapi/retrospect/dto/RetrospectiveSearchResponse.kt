package com.didit.adapter.webapi.retrospect.dto

import com.didit.domain.retrospect.Retrospective
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveSearchResponse(
    val id: UUID,
    val title: String?,
    val feedback: String?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(retrospective: Retrospective): RetrospectiveSearchResponse =
            RetrospectiveSearchResponse(
                id = retrospective.id,
                title = retrospective.title,
                feedback = retrospective.summary?.feedback,
                createdAt = retrospective.createdAt,
            )
    }
}
