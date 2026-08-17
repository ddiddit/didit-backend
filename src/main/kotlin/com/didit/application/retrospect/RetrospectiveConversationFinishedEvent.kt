package com.didit.application.retrospect

import java.util.UUID

data class RetrospectiveConversationFinishedEvent(
    val retrospectiveId: UUID,
    val userId: UUID,
)
