package com.didit.adapter.webapi.retrospect.dto

import com.didit.domain.retrospect.RetroStatus
import com.didit.domain.retrospect.Retrospective
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveDetailResponse(
    val id: UUID,
    val title: String?,
    val status: RetroStatus,
    val summary: SummaryResponse?,
    val completedAt: LocalDateTime?,
) {
    data class SummaryResponse(
        val summary: String,
        val feedback: String,
        val insight: String,
        val doneWork: String,
        val blockedPoint: List<String>,
        val solutionProcess: List<String>,
        val lessonLearned: List<String>,
        val nextAction: List<String>,
    )

    companion object {
        fun from(retrospective: Retrospective): RetrospectiveDetailResponse =
            RetrospectiveDetailResponse(
                id = retrospective.id,
                title = retrospective.title,
                status = retrospective.status,
                summary =
                    retrospective.summary?.let {
                        SummaryResponse(
                            summary = it.summary,
                            feedback = it.feedback,
                            insight = it.insight,
                            doneWork = it.doneWork,
                            blockedPoint = it.blockedPoint.split("\n"),
                            solutionProcess = it.solutionProcess.split("\n"),
                            lessonLearned = it.lessonLearned.split("\n"),
                            nextAction = it.nextAction.split("\n"),
                        )
                    },
                completedAt = retrospective.completedAt,
            )
    }
}
