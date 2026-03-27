package com.didit.application.retrospect.dto

data class GetRetrospectiveResponse(
    val retrospectiveId: String,
    val currentQuestionNumber: Int,
    val isCompleted: Boolean,
    val chatHistory: List<ChatMessageDto>,
    val summary: String? = null,
)

data class ChatMessageDto(
    val questionNumber: Int,
    val content: String,
    val isAnswer: Boolean,
    val isDeepQuestion: Boolean = false,
    val createdAt: String,
)
