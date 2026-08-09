package com.didit.application.retrospect.dto

import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.ConversationStatus
import com.didit.domain.retrospect.ConversationTurnStatus
import com.didit.domain.retrospect.Sender
import com.didit.domain.retrospect.SummaryGenerationStatus
import java.time.LocalDateTime
import java.util.UUID

data class StartConversationV2Result(
    val retrospectiveId: UUID,
    val conversationStatus: ConversationStatus,
    val initialMessage: ConversationMessageResult,
    val readyToComplete: Boolean,
)

data class SubmitConversationMessageResult(
    val turnId: UUID,
    val userMessageId: UUID,
    val assistantMessage: ConversationMessageResult,
    val readyToComplete: Boolean,
)

data class ConversationV2Result(
    val retrospectiveId: UUID,
    val conversationStatus: ConversationStatus,
    val messages: List<ConversationMessageResult>,
    val turns: List<ConversationTurnResult>,
    val readyToComplete: Boolean,
)

data class ConversationMessageResult(
    val id: UUID,
    val sender: Sender,
    val messageType: ConversationMessageType,
    val title: String? = null,
    val body: String? = null,
    val content: String? = null,
    val createdAt: LocalDateTime? = null,
)

data class ConversationTurnResult(
    val id: UUID,
    val clientMessageId: UUID,
    val userMessageId: UUID,
    val status: ConversationTurnStatus,
    val attemptCount: Int,
    val errorCode: String?,
)

data class FinishConversationV2Result(
    val retrospectiveId: UUID,
    val conversationStatus: ConversationStatus,
    val resultGenerationStatus: SummaryGenerationStatus,
)
