package com.didit.adapter.webapi.auth.dto

import com.didit.application.achievement.dto.BadgeResponse
import com.didit.domain.auth.Provider
import com.didit.domain.auth.User
import com.didit.domain.auth.UserAge
import com.didit.domain.auth.UserExperience
import com.didit.domain.shared.Job

data class UserProfileResponseV2(
    val nickname: String?,
    val job: Job?,
    val email: String?,
    val age: UserAge?,
    val experience: UserExperience?,
    val provider: Provider,
    val currentLevel: Int,
    val recentBadges: List<BadgeResponse> = emptyList(),
) {
    companion object {
        fun from(
            user: User,
            currentLevel: Int,
            recentBadges: List<BadgeResponse> = emptyList(),
        ) = UserProfileResponseV2(
            nickname = user.nickname,
            job = user.job,
            email = user.email,
            age = user.age,
            experience = user.experience,
            provider = user.provider,
            currentLevel = currentLevel,
            recentBadges = recentBadges,
        )
    }
}
