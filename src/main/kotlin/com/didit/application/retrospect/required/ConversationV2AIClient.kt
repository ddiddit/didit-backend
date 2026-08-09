package com.didit.application.retrospect.required

import com.didit.domain.auth.UserExperience
import com.didit.domain.retrospect.MessageRelevance
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.retrospect.Sender
import com.didit.domain.shared.Job
import java.util.UUID

interface ConversationV2AIClient {
    fun generateConversationTurn(request: ConversationTurnAIRequest): GeneratedConversationTurn
}

data class ConversationTurnAIRequest(
    val job: Job?,
    val experience: UserExperience?,
    val messages: List<ConversationContextMessage>,
    val analysisItems: List<ConversationAnalysisItem>,
    val currentMessageId: UUID,
    val consecutiveIrrelevantCount: Int,
)

data class ConversationContextMessage(
    val id: UUID,
    val sender: Sender,
    val content: String,
)

data class ConversationAnalysisItem(
    val itemType: RetrospectiveItemType,
    val status: RetrospectiveItemStatus,
    val summary: String?,
)

data class GeneratedConversationTurn(
    val acknowledgement: String,
    val interpretation: String,
    val question: String,
    val questionTarget: RetrospectiveItemType?,
    val relevance: MessageRelevance,
    val analysisUpdates: List<ConversationAnalysisUpdate>,
    val inputTokens: Int,
    val outputTokens: Int,
) {
    fun content(): String =
        listOf(acknowledgement, interpretation, question).map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
}

data class ConversationAnalysisUpdate(
    val itemType: RetrospectiveItemType,
    val status: RetrospectiveItemStatus,
    val summary: String,
    val evidenceMessageIds: List<UUID>,
)
