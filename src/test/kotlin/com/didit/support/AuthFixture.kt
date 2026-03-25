package com.didit.support

import com.didit.domain.auth.Provider
import com.didit.domain.auth.RefreshToken
import com.didit.domain.auth.User
import com.didit.domain.auth.UserConsent
import java.time.LocalDateTime
import java.util.UUID

object UserFixture {
    fun create(
        provider: Provider = Provider.KAKAO,
        providerId: String = "kakao-123",
        nickname: String = "디딧유저",
    ) = User.register(provider = provider, providerId = providerId, nickname = nickname)
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
