package com.didit.application.admin.required

import com.didit.application.retrospect.required.ConversationTurnAIRequest
import com.didit.application.retrospect.required.GeneratedConversationTurn

interface AdminPromptPreviewAIClient {
    fun preview(
        template: String,
        request: ConversationTurnAIRequest,
    ): GeneratedConversationTurn
}
