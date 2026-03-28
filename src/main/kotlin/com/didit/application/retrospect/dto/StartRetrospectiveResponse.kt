package com.didit.application.retrospect.dto

data class StartRetrospectiveResponse(
    val retrospectiveId: String,
    val questionNumber: Int,
    val question: String,
    val isDeepQuestion: Boolean = false,
)
