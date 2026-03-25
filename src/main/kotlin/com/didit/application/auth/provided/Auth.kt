package com.didit.application.auth.provided

import com.didit.application.auth.dto.RefreshResponse
import com.didit.application.auth.dto.TokenResponse
import com.didit.domain.auth.Provider
import java.util.UUID

interface Auth {
    fun login(
        provider: Provider,
        oauthToken: String,
    ): TokenResponse

    fun logout(userId: UUID)

    fun withdraw(userId: UUID)

    fun refresh(refreshToken: String): RefreshResponse
}
