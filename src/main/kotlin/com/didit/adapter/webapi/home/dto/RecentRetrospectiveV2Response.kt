package com.didit.adapter.webapi.home.dto

import com.didit.adapter.webapi.organization.dto.TagListResponse
import com.didit.application.retrospect.dto.RetrospectiveDetailResult
import java.time.LocalDateTime
import java.util.UUID

// 홈 "최근 제안 받은 행동" 카드 전용 — 회고 요약이 아니라 다음 행동 제안 요약구(nextActionTitle)를 노출한다.
// 회고 목록/프로젝트별/태그별 목록은 회고 요약을 그대로 보여줘야 하므로 공유 DTO와 분리한다.
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
