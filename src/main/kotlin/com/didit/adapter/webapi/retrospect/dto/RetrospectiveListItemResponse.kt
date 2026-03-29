package com.didit.adapter.webapi.retrospect.dto

import com.didit.domain.retrospect.Retrospective
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveListItemResponse(
    val id: UUID,
    val title: String?,
    val projectId: UUID?,
    val feedback: String?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(retrospective: Retrospective): RetrospectiveListItemResponse =
            RetrospectiveListItemResponse(
                id = retrospective.id,
                title = retrospective.title,
                projectId = retrospective.projectId,
                feedback = retrospective.summary?.feedback,
                createdAt = retrospective.createdAt,
            )
    }
}
