package com.didit.adapter.webapi.retrospect.dto

import com.didit.application.retrospect.dto.AISummaryResponse

data class CompleteRetrospectiveResponse(
    val title: String,
    val content: ContentResponse,
) {
    companion object {
        fun from(aiSummaryResponse: AISummaryResponse): CompleteRetrospectiveResponse =
            CompleteRetrospectiveResponse(
                title = aiSummaryResponse.title,
                content =
                    ContentResponse(
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
