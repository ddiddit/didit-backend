package com.didit.application.retrospect.provided

import com.didit.application.retrospect.dto.ConversationV2Result
import com.didit.application.retrospect.dto.FinishConversationV2Result
import com.didit.application.retrospect.dto.StartConversationV2Result
import com.didit.application.retrospect.dto.SubmitConversationMessageResult
import com.didit.domain.retrospect.InputType
import java.util.UUID

interface RetrospectiveConversationV2 {
    fun start(userId: UUID): StartConversationV2Result

    fun submitMessage(
        retrospectiveId: UUID,
        userId: UUID,
        clientMessageId: UUID,
        content: String,
    ): SubmitConversationMessageResult = submitMessage(retrospectiveId, userId, clientMessageId, content, InputType.TEXT)

    fun submitMessage(
        retrospectiveId: UUID,
        userId: UUID,
        clientMessageId: UUID,
        content: String,
        inputType: InputType,
    ): SubmitConversationMessageResult

    fun getConversation(
        retrospectiveId: UUID,
        userId: UUID,
    ): ConversationV2Result

    fun finish(
        retrospectiveId: UUID,
        userId: UUID,
    ): FinishConversationV2Result
}
