package com.didit.adapter.persistence.audit

import java.time.LocalDateTime
import java.util.UUID

interface LastLoginProjection {
    val userId: UUID
    val lastLoginAt: LocalDateTime
}
