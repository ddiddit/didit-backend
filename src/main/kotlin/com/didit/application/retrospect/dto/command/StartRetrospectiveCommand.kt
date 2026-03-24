package com.didit.application.retrospect.dto.command

import java.util.UUID

data class StartRetrospectiveCommand(
    val userId: UUID,
    val projectId: UUID?,
    val tagIds: List<UUID> = emptyList(),
)
