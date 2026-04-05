package com.didit.adapter.webapi.retrospect.dto

import com.didit.application.retrospect.dto.AISummaryResponse

data class CompleteRetrospectiveResponse(
    val title: String,
    val summary: SummaryResponse,
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
        fun from(aiSummaryResponse: AISummaryResponse): CompleteRetrospectiveResponse =
            CompleteRetrospectiveResponse(
                title = aiSummaryResponse.title,
                summary =
                    SummaryResponse(
                        summary = aiSummaryResponse.summary,
                        feedback = aiSummaryResponse.feedback,
                        insight = aiSummaryResponse.insight,
                        doneWork = aiSummaryResponse.doneWork,
                        blockedPoint = aiSummaryResponse.blockedPoint,
                        solutionProcess = aiSummaryResponse.solutionProcess,
                        lessonLearned = aiSummaryResponse.lessonLearned,
                        nextAction = aiSummaryResponse.nextAction,
                    ),
            )
    }
}
