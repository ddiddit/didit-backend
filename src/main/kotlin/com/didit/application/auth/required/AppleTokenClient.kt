package com.didit.application.auth.required

import com.didit.application.auth.dto.AppleTokenExchange

interface AppleTokenClient {
    fun exchangeNativeAuthorizationCode(
        idToken: String,
        authorizationCode: String,
    ): AppleTokenExchange

    fun exchangeWebAuthorizationCode(
        authorizationCode: String,
        redirectUri: String?,
        expectedNonce: String?,
    ): AppleTokenExchange

    fun revoke(
        tokenClientId: String,
        refreshToken: String,
    )
}
