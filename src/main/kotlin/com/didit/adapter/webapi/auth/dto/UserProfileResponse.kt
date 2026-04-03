package com.didit.adapter.webapi.auth.dto

import com.didit.application.achievement.dto.BadgeResponse
import com.didit.domain.auth.Provider
import com.didit.domain.auth.User
import com.didit.domain.shared.Job

data class UserProfileResponse(
    val nickname: String?,
    val job: Job?,
    val email: String?,
    val provider: Provider,
    val recentBadges: List<BadgeResponse> = emptyList(),
) {
    companion object {
        fun from(
            user: User,
            recentBadges: List<BadgeResponse> = emptyList(),
        ) = UserProfileResponse(
            nickname = user.nickname,
            job = user.job,
            email = user.email,
            provider = user.provider,
            recentBadges = recentBadges,
        )
    }
}
