package com.didit.application.auth.dto

import com.didit.domain.auth.Provider
import com.didit.domain.auth.SocialCredentialType

data class SocialLoginCommand(
    val provider: Provider,
    val credentialType: SocialCredentialType,
    val credential: String,
    val redirectUri: String? = null,
    val nonce: String? = null,
    val authorizationCode: String? = null,
)
