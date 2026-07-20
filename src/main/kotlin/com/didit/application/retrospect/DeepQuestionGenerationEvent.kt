package com.didit.application.retrospect

import java.util.UUID

data class DeepQuestionGenerationEvent(
    val retrospectiveId: UUID,
    val userId: UUID,
)
