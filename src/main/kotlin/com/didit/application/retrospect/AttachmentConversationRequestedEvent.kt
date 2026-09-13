package com.didit.application.retrospect

import java.util.UUID

data class AttachmentConversationRequestedEvent(
    val retrospectiveId: UUID,
    val userId: UUID,
    val turnId: UUID,
    val userMessageId: UUID,
    val recovery: Boolean = false,
)
