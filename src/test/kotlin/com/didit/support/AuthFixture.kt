package com.didit.support

import com.didit.domain.auth.Provider
import com.didit.domain.auth.RefreshToken
import com.didit.domain.auth.User
import com.didit.domain.auth.UserConsent
import com.didit.domain.auth.UserRegisterRequest
import com.didit.domain.shared.Job
import java.time.LocalDateTime
import java.util.UUID

object UserFixture {
    fun create(
        provider: Provider = Provider.KAKAO,
        providerId: String = "kakao-0325",
        email: String? = "test@kakao.com",
    ) = User.register(
        UserRegisterRequest(
            provider = provider,
            providerId = providerId,
            email = email,
        ),
    )

    fun createOnboarded(
        provider: Provider = Provider.KAKAO,
        providerId: String = "kakao-0325",
        email: String? = "test@kakao.com",
        nickname: String = "디딧유저",
        job: Job = Job.DEVELOPER,
    ) = create(provider, providerId, email).apply {
        completeOnboarding(nickname = nickname, job = job)
    }

    fun createOnboardedWithConsent(
        provider: Provider = Provider.KAKAO,
        providerId: String = "kakao-0325",
        email: String? = "test@kakao.com",
        nickname: String = "디딧유저",
        job: Job = Job.DEVELOPER,
        marketingAgreed: Boolean = false,
    ) = createOnboarded(provider, providerId, email, nickname, job).apply {
        createConsent(marketingAgreed = marketingAgreed)
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
