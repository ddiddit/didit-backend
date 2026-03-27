package com.didit.application.retrospect.required

import com.didit.domain.auth.Job

interface AIClient {
    fun generateDeepQuestion(
        job: Job,
        answers: List<String>,
    ): String

    fun generateSummary(
        job: Job,
        allAnswers: List<String>,
    ): String
}
