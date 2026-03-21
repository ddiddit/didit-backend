package com.didit.application.retrospect.dto.command

import java.util.UUID

data class SubmitAnswerCommand(
    val retrospectiveId: UUID,
    val answer: String
)