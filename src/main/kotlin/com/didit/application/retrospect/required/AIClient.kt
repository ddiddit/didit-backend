package com.didit.application.retrospect.required

import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.domain.shared.Job

interface AIClient {
    fun generateDeepQuestion(
        job: Job?,
        answers: List<String>,
    ): String

    fun generateSummaryWithTitle(
        job: Job?,
        allAnswers: List<String>,
    ): AISummaryResponse
}
