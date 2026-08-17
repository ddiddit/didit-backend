package com.didit.adapter.webapi.retrospect.dto

import com.didit.application.retrospect.dto.ConversationMessageResult
import com.didit.application.retrospect.dto.ConversationTurnResult
import com.didit.application.retrospect.dto.ConversationV2Result
import com.didit.application.retrospect.dto.FinishConversationV2Result
import com.didit.application.retrospect.dto.StartConversationV2Result
import com.didit.application.retrospect.dto.SubmitConversationMessageResult
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.ConversationStatus
import com.didit.domain.retrospect.ConversationTurnStatus
import com.didit.domain.retrospect.Sender
import com.didit.domain.retrospect.SummaryGenerationStatus
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import java.util.UUID

data class SubmitConversationMessageV2Request(
    val clientMessageId: UUID,
    @field:NotBlank
    val content: String,
)

data class StartConversationV2Response(
    val retrospectiveId: UUID,
    val conversationStatus: ConversationStatus,
    val initialMessage: ConversationMessageV2Response,
    val readyToComplete: Boolean,
) {
    companion object {
        fun from(result: StartConversationV2Result) =
            StartConversationV2Response(
                retrospectiveId = result.retrospectiveId,
                conversationStatus = result.conversationStatus,
                initialMessage = ConversationMessageV2Response.from(result.initialMessage),
                readyToComplete = result.readyToComplete,
            )
    }
}

data class SubmitConversationMessageV2Response(
    val turnId: UUID,
    val userMessageId: UUID,
    val assistantMessage: ConversationMessageV2Response,
    val readyToComplete: Boolean,
) {
    companion object {
        fun from(result: SubmitConversationMessageResult) =
            SubmitConversationMessageV2Response(
                turnId = result.turnId,
                userMessageId = result.userMessageId,
                assistantMessage = ConversationMessageV2Response.from(result.assistantMessage),
                readyToComplete = result.readyToComplete,
            )
    }
}

data class ConversationV2Response(
    val retrospectiveId: UUID,
    val conversationStatus: ConversationStatus,
    val messages: List<ConversationMessageV2Response>,
    val turns: List<ConversationTurnV2Response>,
    val readyToComplete: Boolean,
) {
    companion object {
        fun from(result: ConversationV2Result) =
            ConversationV2Response(
                retrospectiveId = result.retrospectiveId,
                conversationStatus = result.conversationStatus,
                messages = result.messages.map(ConversationMessageV2Response::from),
                turns = result.turns.map(ConversationTurnV2Response::from),
                readyToComplete = result.readyToComplete,
            )
    }
}

data class ConversationMessageV2Response(
    val id: UUID,
    val sender: Sender,
    val messageType: ConversationMessageType,
    val title: String?,
    val body: String?,
    val content: String?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(result: ConversationMessageResult) =
            ConversationMessageV2Response(
                id = result.id,
                sender = result.sender,
                messageType = result.messageType,
                title = result.title,
                body = result.body,
                content = result.content,
                createdAt = result.createdAt,
            )
    }
}

data class ConversationTurnV2Response(
    val id: UUID,
    val clientMessageId: UUID,
    val userMessageId: UUID,
    val status: ConversationTurnStatus,
    val attemptCount: Int,
    val errorCode: String?,
) {
    companion object {
        fun from(result: ConversationTurnResult) =
            ConversationTurnV2Response(
                id = result.id,
                clientMessageId = result.clientMessageId,
                userMessageId = result.userMessageId,
                status = result.status,
                attemptCount = result.attemptCount,
                errorCode = result.errorCode,
            )
    }
}

data class FinishConversationV2Response(
    val retrospectiveId: UUID,
    val conversationStatus: ConversationStatus,
    val resultGenerationStatus: SummaryGenerationStatus,
) {
    companion object {
        fun from(result: FinishConversationV2Result) =
            FinishConversationV2Response(
                retrospectiveId = result.retrospectiveId,
                conversationStatus = result.conversationStatus,
                resultGenerationStatus = result.resultGenerationStatus,
            )
    }
}
