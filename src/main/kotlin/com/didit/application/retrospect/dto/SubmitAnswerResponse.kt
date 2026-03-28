package com.didit.application.retrospect.dto

data class SubmitAnswerResponse(
    val retrospectiveId: String,
    val questionNumber: Int,
    val nextQuestion: String?,
    val isDeepQuestion: Boolean = false,
    val isCompleted: Boolean = false,
    val summary: String? = null,
)
