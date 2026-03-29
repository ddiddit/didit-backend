package com.didit.adapter.webapi.retrospect.dto

import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import java.util.UUID

data class StartRetrospectiveResponse(
    val retrospectiveId: UUID,
    val firstQuestionType: QuestionType,
    val firstQuestionContent: String,
) {
    companion object {
        fun from(retrospective: Retrospective): StartRetrospectiveResponse {
            val firstMessage = retrospective.chatMessages.first()
            return StartRetrospectiveResponse(
                retrospectiveId = retrospective.id,
                firstQuestionType = firstMessage.questionType,
                firstQuestionContent = firstMessage.content,
            )
        }
    }
}
