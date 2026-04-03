package com.didit.application.auth.provided

import com.didit.domain.auth.User
import com.didit.domain.shared.Job
import java.util.UUID

interface UserFinder {
    fun findByIdOrThrow(userId: UUID): User

    fun existsByNickname(nickname: String): Boolean

    fun getJobByUserId(userId: UUID): Job?
}
