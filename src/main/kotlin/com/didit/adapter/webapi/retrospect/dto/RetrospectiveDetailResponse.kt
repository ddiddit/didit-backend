package com.didit.adapter.webapi.retrospect.dto

import com.didit.domain.retrospect.RetroStatus
import com.didit.domain.retrospect.Retrospective
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveDetailResponse(
    val id: UUID,
    val title: String?,
    val status: RetroStatus,
    val content: ContentResponse?,
    val completedAt: LocalDateTime?,
) {
    companion object {
        fun from(retrospective: Retrospective): RetrospectiveDetailResponse =
            RetrospectiveDetailResponse(
                id = retrospective.id,
                title = retrospective.title,
                status = retrospective.status,
                content =
                    retrospective.summary?.let {
                        ContentResponse(
                            summary = it.summary,
                            blockedPoint = it.blockedPoint.split("\n"),
                            solutionProcess = it.solutionProcess.split("\n"),
                            lessonLearned = it.lessonLearned.split("\n"),
                            insight =
                                InsightContentResponse(
                                    title = it.insightTitle,
                                    description = it.insightDescription,
                                ),
                            nextAction =
                                NextActionContentResponse(
                                    title = it.nextActionTitle,
                                    description = it.nextActionDescription,
                                ),
                        )
                    },
                completedAt = retrospective.completedAt,
            )
    }
}
