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
                        blockedPoint = aiSummaryResponse.blockedPoint,
                        solutionProcess = aiSummaryResponse.solutionProcess,
                        lessonLearned = aiSummaryResponse.lessonLearned,
                        insight =
                            InsightContentResponse(
                                title = aiSummaryResponse.insight.title,
                                description = aiSummaryResponse.insight.description,
                            ),
                        nextAction =
                            NextActionContentResponse(
                                title = aiSummaryResponse.nextAction.title,
                                description = aiSummaryResponse.nextAction.description,
                            ),
                    ),
            )
    }
}
