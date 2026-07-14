package com.didit.adapter.webapi.home.dto

import com.didit.adapter.webapi.organization.dto.TagListResponse
import com.didit.application.retrospect.dto.RetrospectiveDetailResult
import java.time.LocalDateTime
import java.util.UUID

data class RecentRetrospectiveV2Response(
    val id: UUID,
    val title: String?,
    val nextAction: String?,
    val completedAt: LocalDateTime?,
    val projectName: String?,
    val tags: List<TagListResponse>,
) {
    companion object {
        fun from(result: RetrospectiveDetailResult): RecentRetrospectiveV2Response =
            RecentRetrospectiveV2Response(
                id = result.retrospective.id,
                title = result.retrospective.title,
                nextAction = result.retrospective.summary?.nextActionTitle,
                completedAt = result.retrospective.completedAt,
                projectName = result.project?.name,
                tags = result.tags.map { TagListResponse.from(it) },
            )
    }
}
