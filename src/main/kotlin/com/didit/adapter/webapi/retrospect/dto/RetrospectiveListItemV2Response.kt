package com.didit.adapter.webapi.retrospect.dto

import com.didit.adapter.webapi.organization.dto.TagListResponse
import com.didit.application.retrospect.dto.RetrospectiveDetailResult
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveListItemV2Response(
    val id: UUID,
    val title: String?,
    val summary: String?,
    val completedAt: LocalDateTime?,
    val projectName: String?,
    val tags: List<TagListResponse>,
) {
    companion object {
        fun from(result: RetrospectiveDetailResult): RetrospectiveListItemV2Response =
            RetrospectiveListItemV2Response(
                id = result.retrospective.id,
                title = result.retrospective.title,
                summary = result.retrospective.summary?.summary,
                completedAt = result.retrospective.completedAt,
                projectName = result.project?.name,
                tags = result.tags.map { TagListResponse.from(it) },
            )
    }
}
