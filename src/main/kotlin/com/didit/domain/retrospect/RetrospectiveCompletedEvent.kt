package com.didit.domain.retrospect

import java.time.LocalDate
import java.util.UUID

data class RetrospectiveCompletedEvent(
    val userId: UUID,
    val retroDate: LocalDate,
)
