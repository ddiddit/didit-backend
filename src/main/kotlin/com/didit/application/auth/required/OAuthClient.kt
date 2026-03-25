package com.didit.application.auth.required

import com.didit.application.auth.dto.UserInfo
import com.didit.domain.auth.Provider

interface OAuthClient {
    fun supports(provider: Provider): Boolean

    fun getUserInfo(oauthToken: String): UserInfo
}
