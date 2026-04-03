package com.didit.application.retrospect.dto

import com.didit.domain.retrospect.QuestionType

data class SubmitAnswerResponse(
    val content: String? = null,
    val nextQuestionType: QuestionType?,
    val nextQuestionContent: String?,
    val isReadyToComplete: Boolean,
)
