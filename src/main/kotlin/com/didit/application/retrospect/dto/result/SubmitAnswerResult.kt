package com.didit.application.retrospect.dto.result

import com.didit.domain.retrospect.enums.QuestionType

data class SubmitAnswerResult(
    val completed: Boolean,
    val questionType: QuestionType? = null,
    val question: String? = null,
    val summary: RetrospectiveSummaryResult? = null
)