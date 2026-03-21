package com.didit.adapter.retrospect.`in`.web.request

import java.util.UUID

data class StartRetrospectiveRequest(
    val userId: UUID,
    val projectId: UUID?,
    val tagIds: List<UUID> = emptyList()
)