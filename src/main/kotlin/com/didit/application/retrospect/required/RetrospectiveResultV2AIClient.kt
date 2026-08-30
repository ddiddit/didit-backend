package com.didit.application.retrospect.required

import com.didit.domain.auth.UserExperience
import com.didit.domain.shared.Job

interface RetrospectiveResultV2AIClient {
    fun generateResult(request: RetrospectiveResultV2AIRequest): GeneratedRetrospectiveResultV2
}

data class RetrospectiveResultV2AIRequest(
    val job: Job?,
    val experience: UserExperience?,
    val messages: List<ConversationContextMessage>,
    val analysisItems: List<ConversationAnalysisItem>,
)

data class GeneratedRetrospectiveResultV2(
    val title: String,
    val summary: String?,
    val strength: String?,
    val improvement: String?,
    val process: String?,
    val learning: String?,
    val insight: String?,
    val nextActions: List<String>?,
    val inputTokens: Int,
    val outputTokens: Int,
)
