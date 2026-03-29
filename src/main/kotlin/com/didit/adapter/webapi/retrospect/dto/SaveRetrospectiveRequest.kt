package com.didit.adapter.webapi.retrospect.dto

import com.didit.application.retrospect.dto.AISummaryResponse

data class SaveRetrospectiveRequest(
    val title: String,
    val summary: SummaryRequest,
) {
    data class SummaryRequest(
        val feedback: String,
        val insight: String,
        val doneWork: String,
        val blockedPoint: String,
        val solutionProcess: String,
        val lessonLearned: String,
    )

    fun toAISummaryResponse(): AISummaryResponse =
        AISummaryResponse(
            title = title,
            feedback = summary.feedback,
            insight = summary.insight,
            doneWork = summary.doneWork,
            blockedPoint = summary.blockedPoint,
            solutionProcess = summary.solutionProcess,
            lessonLearned = summary.lessonLearned,
        )
}
