package com.didit.application.auth.required

import com.didit.application.auth.dto.UserInfo
import com.didit.application.auth.exception.InvalidSocialCredentialTypeException
import com.didit.domain.auth.SocialCredentialType

interface OAuthClient {
    fun getUserInfo(oauthToken: String): UserInfo

    fun getUserInfo(
        credentialType: SocialCredentialType,
        credential: String,
    ): UserInfo = throw InvalidSocialCredentialTypeException()

    fun getUserInfo(
        credentialType: SocialCredentialType,
        credential: String,
        redirectUri: String?,
    ): UserInfo = getUserInfo(credentialType, credential)
}
