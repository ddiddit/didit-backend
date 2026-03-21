package com.didit.adapter.retrospect.out.ai.dto

data class ClovaStudioChatRequest(
    val messages: List<ClovaStudioChatMessageDto>,
    val topP: Double = 0.8,
    val topK: Int = 0,
    val maxTokens: Int = 700,
    val temperature: Double = 0.5,
    val repeatPenalty: Double = 1.1,
    val stopBefore: List<String> = emptyList(),
    val includeAiFilters: Boolean = false,
    val seed: Int = 0
)