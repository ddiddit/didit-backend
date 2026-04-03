package com.didit.domain.admin

import java.time.LocalDateTime
import java.util.UUID

data class AdminInviteCreateRequest(
    val email: String,
    val position: AdminPosition,
    val invitedBy: UUID,
    val expiredAt: LocalDateTime,
)
