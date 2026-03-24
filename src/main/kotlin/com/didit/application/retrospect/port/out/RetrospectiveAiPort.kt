package com.didit.application.retrospect.port.out

import com.didit.domain.retrospect.entity.ChatMessage
import com.didit.domain.retrospect.model.RetrospectiveSummary

interface RetrospectiveAiPort {
    fun analyzeAnswer(messages: List<ChatMessage>): AiAnalyzeResult

    fun generateDeepQuestion(messages: List<ChatMessage>): AiDeepQuestionResult

    fun generateSummary(messages: List<ChatMessage>): AiSummaryResult
}

data class AiAnalyzeResult(
    val inputTokens: Int,
    val outputTokens: Int,
)

data class AiDeepQuestionResult(
    val question: String,
    val inputTokens: Int,
    val outputTokens: Int,
)

data class AiSummaryResult(
    val summary: RetrospectiveSummary,
    val inputTokens: Int,
    val outputTokens: Int,
)
