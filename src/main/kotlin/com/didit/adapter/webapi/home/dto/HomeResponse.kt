package com.didit.adapter.webapi.home.dto

import com.didit.domain.retrospect.Retrospective
import java.time.LocalDateTime
import java.util.UUID

data class HomeResponse(
    val nickname: String,
    val todayRetrospectiveCount: Int,
    val recentRetrospectives: List<RecentRetrospectiveResponse>,
    val latestFeedback: String?,
) {
    data class RecentRetrospectiveResponse(
        val id: UUID,
        val title: String?,
        val feedback: String?,
        val completedAt: LocalDateTime?,
    ) {
        companion object {
            fun from(retrospective: Retrospective): RecentRetrospectiveResponse =
                RecentRetrospectiveResponse(
                    id = retrospective.id,
                    title = retrospective.title,
                    feedback = retrospective.summary?.feedback,
                    completedAt = retrospective.completedAt,
                )
        }
    }
}
