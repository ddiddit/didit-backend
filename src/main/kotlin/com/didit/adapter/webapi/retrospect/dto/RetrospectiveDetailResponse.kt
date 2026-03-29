package com.didit.adapter.webapi.retrospect.dto

import com.didit.domain.retrospect.RetroStatus
import com.didit.domain.retrospect.Retrospective
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveDetailResponse(
    val id: UUID,
    val title: String?,
    val projectId: UUID?,
    val status: RetroStatus,
    val summary: SummaryResponse?,
    val completedAt: LocalDateTime?,
) {
    data class SummaryResponse(
        val feedback: String,
        val insight: String,
        val doneWork: String,
        val blockedPoint: String,
        val solutionProcess: String,
        val lessonLearned: String,
    )

    companion object {
        fun from(retrospective: Retrospective): RetrospectiveDetailResponse =
            RetrospectiveDetailResponse(
                id = retrospective.id,
                title = retrospective.title,
                projectId = retrospective.projectId,
                status = retrospective.status,
                summary =
                    retrospective.summary?.let {
                        SummaryResponse(
                            feedback = it.feedback,
                            insight = it.insight,
                            doneWork = it.doneWork,
                            blockedPoint = it.blockedPoint,
                            solutionProcess = it.solutionProcess,
                            lessonLearned = it.lessonLearned,
                        )
                    },
                completedAt = retrospective.updatedAt,
            )
    }
}
