package com.didit.support

import com.didit.domain.auth.Job
import com.didit.domain.auth.Provider
import com.didit.domain.auth.RefreshToken
import com.didit.domain.auth.User
import com.didit.domain.auth.UserConsent
import com.didit.domain.auth.UserRegisterRequest
import java.time.LocalDateTime
import java.util.UUID

object UserFixture {
    fun create(
        provider: Provider = Provider.KAKAO,
        providerId: String = "kakao-0325",
    ) = User.register(
        UserRegisterRequest(
            provider = provider,
            providerId = providerId,
        ),
    )

    fun createOnboarded(
        provider: Provider = Provider.KAKAO,
        providerId: String = "kakao-0325",
        nickname: String = "디딧유저",
        job: Job = Job.DEVELOPER,
    ) = create(provider, providerId).apply {
        completeOnboarding(nickname = nickname, job = job)
    }
}

object UserConsentFixture {
    fun create(
        userId: UUID = UUID.randomUUID(),
        marketingAgreed: Boolean = false,
    ) = UserConsent.create(userId = userId, marketingAgreed = marketingAgreed)
}

object RefreshTokenFixture {
    fun create(
        userId: UUID = UUID.randomUUID(),
        token: String = "refresh-token",
        expiresAt: LocalDateTime = LocalDateTime.now().plusDays(14),
    ) = RefreshToken.create(userId = userId, token = token, expiresAt = expiresAt)
}
