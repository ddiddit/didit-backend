package com.didit.application.admin.provided

import com.didit.domain.auth.User
import com.didit.domain.auth.UserAge
import com.didit.domain.auth.UserExperience
import com.didit.domain.shared.Job
import java.time.LocalDateTime
import java.util.UUID

data class UserListResult(
    val content: List<UserSummary>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class UserSummary(
    val id: UUID,
    val email: String?,
    val nickname: String?,
    val job: Job?,
    val age: UserAge?,
    val experience: UserExperience?,
    val provider: String,
    val createdAt: LocalDateTime?,
    val lastLoginAt: LocalDateTime?,
    val onboardingCompleted: Boolean,
    val deleted: Boolean,
) {
    companion object {
        fun from(
            user: User,
            lastLoginAt: LocalDateTime?,
        ) = UserSummary(
            id = user.id,
            email = user.email,
            nickname = user.nickname,
            job = user.job,
            age = user.age,
            experience = user.experience,
            provider = user.provider.name,
            createdAt = user.createdAt,
            lastLoginAt = lastLoginAt,
            onboardingCompleted = user.isOnboardingCompleted,
            deleted = user.isDeleted,
        )
    }
}
