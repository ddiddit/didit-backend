package com.didit.domain.auth

import java.time.LocalDate
import java.util.UUID

data class UserLoggedInEvent(
    val userId: UUID,
    val accessDateKst: LocalDate,
)
