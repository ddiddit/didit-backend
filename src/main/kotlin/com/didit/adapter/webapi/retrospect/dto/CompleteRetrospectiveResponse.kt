package com.didit.adapter.webapi.retrospect.dto

import com.didit.application.retrospect.dto.AISummaryResponse

data class CompleteRetrospectiveResponse(
    val title: String,
    val summary: SummaryResponse,
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
        fun from(aiSummaryResponse: AISummaryResponse): CompleteRetrospectiveResponse =
            CompleteRetrospectiveResponse(
                title = aiSummaryResponse.title,
                summary =
                    SummaryResponse(
                        feedback = aiSummaryResponse.feedback,
                        insight = aiSummaryResponse.insight,
                        doneWork = aiSummaryResponse.doneWork,
                        blockedPoint = aiSummaryResponse.blockedPoint,
                        solutionProcess = aiSummaryResponse.solutionProcess,
                        lessonLearned = aiSummaryResponse.lessonLearned,
                    ),
            )
    }
}
