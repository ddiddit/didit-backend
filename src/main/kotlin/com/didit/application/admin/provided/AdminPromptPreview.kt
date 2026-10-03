package com.didit.application.admin.provided

import com.didit.domain.auth.UserExperience
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.ConversationUserIntent
import com.didit.domain.retrospect.MessageRelevance
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.retrospect.Sender
import com.didit.domain.shared.Job
import java.util.UUID

interface AdminPromptPreview {
    fun preview(command: AdminPromptPreviewCommand): AdminPromptPreviewResult
}

enum class AdminPromptSource {
    SAVED,
    DRAFT,
}

data class AdminPromptPreviewCommand(
    val job: Job,
    val experience: UserExperience,
    val promptSource: AdminPromptSource,
    val draftPrompt: String?,
    val priorState: AdminPromptPreviewState?,
    val userMessageId: UUID,
    val message: String,
    val conversationIntent: ConversationUserIntent = ConversationUserIntent.NORMAL,
)

data class AdminPromptPreviewState(
    val messages: List<AdminPromptPreviewMessage>,
    val analysisItems: List<AdminPromptPreviewAnalysisItem>,
    val recentQuestionTargets: List<RetrospectiveItemType> = emptyList(),
    val completedQuestionCount: Int = 0,
    val completionPreviouslyOffered: Boolean = false,
)

data class AdminPromptPreviewMessage(
    val id: UUID,
    val sender: Sender,
    val content: String,
    val messageType: ConversationMessageType,
    val supportingContent: String? = null,
    val relevance: MessageRelevance? = null,
)

data class AdminPromptPreviewAnalysisItem(
    val itemType: RetrospectiveItemType,
    val status: RetrospectiveItemStatus,
    val summary: String?,
    val questionAllowed: Boolean = true,
)

data class AdminPromptPreviewProgress(
    val filledCount: Int,
    val totalCount: Int,
)

data class AdminPromptPreviewResult(
    val assistantMessage: AdminPromptPreviewMessage,
    val progress: AdminPromptPreviewProgress,
    val nextState: AdminPromptPreviewState,
)
