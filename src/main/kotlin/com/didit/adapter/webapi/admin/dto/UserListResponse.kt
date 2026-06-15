package com.didit.adapter.webapi.admin.dto

import com.didit.application.admin.provided.UserSummary
import com.didit.domain.auth.UserAge
import com.didit.domain.auth.UserExperience
import com.didit.domain.shared.Job
import java.time.LocalDateTime
import java.util.UUID

data class UserListResponse(
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
        fun from(summary: UserSummary) =
            UserListResponse(
                id = summary.id,
                email = summary.email,
                nickname = summary.nickname,
                job = summary.job,
                age = summary.age,
                experience = summary.experience,
                provider = summary.provider,
                createdAt = summary.createdAt,
                lastLoginAt = summary.lastLoginAt,
                onboardingCompleted = summary.onboardingCompleted,
                deleted = summary.deleted,
            )
    }
}
