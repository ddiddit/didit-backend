package com.didit.adapter.webapi.retrospect.dto

import com.didit.domain.retrospect.Retrospective
import java.time.LocalDateTime
import java.util.UUID

data class DailyRetrospectiveResponse(
    val id: UUID,
    val title: String?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(retrospective: Retrospective): DailyRetrospectiveResponse =
            DailyRetrospectiveResponse(
                id = retrospective.id,
                title = retrospective.title,
                createdAt = retrospective.createdAt,
            )
    }
}
