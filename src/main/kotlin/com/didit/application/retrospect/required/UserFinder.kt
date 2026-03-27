package com.didit.application.retrospect.required

import com.didit.domain.auth.User
import java.util.UUID

interface UserFinder {
    fun findByIdOrThrow(userId: UUID): User
}
