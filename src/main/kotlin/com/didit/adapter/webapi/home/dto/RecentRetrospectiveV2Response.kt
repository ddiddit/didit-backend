package com.didit.adapter.webapi.home.dto

import com.didit.adapter.webapi.organization.dto.TagListResponse
import com.didit.application.retrospect.dto.RetrospectiveDetailResult
import com.didit.domain.retrospect.toActionTitle
import com.didit.domain.retrospect.toDisplayActionTitle
import java.time.LocalDateTime
import java.util.UUID

data class RecentRetrospectiveV2Response(
    val id: UUID,
    val title: String?,
    val nextAction: String,
    val completedAt: LocalDateTime?,
    val projectName: String?,
    val tags: List<TagListResponse>,
) {
    companion object {
        fun from(result: RetrospectiveDetailResult): RecentRetrospectiveV2Response {
            val retrospective = result.retrospective
            val nextAction =
                if (retrospective.isV2()) {
                    retrospective.resultV2
                        ?.nextActions
                        ?.firstOrNull()
                        ?.toDisplayActionTitle()
                } else {
                    retrospective.summary?.nextActionTitle?.toActionTitle()
                }?.trim()?.takeIf { it.isNotEmpty() }

            return RecentRetrospectiveV2Response(
                id = result.retrospective.id,
                title = result.retrospective.title,
                nextAction = checkNotNull(nextAction) { "최근 회고에는 다음 행동이 필요합니다." },
                completedAt = result.retrospective.completedAt,
                projectName = result.project?.name,
                tags = result.tags.map { TagListResponse.from(it) },
            )
        }
    }
}
