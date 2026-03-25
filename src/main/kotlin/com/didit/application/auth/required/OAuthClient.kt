package com.didit.application.auth.required

import com.didit.application.auth.dto.UserInfo

interface OAuthClient {
    fun getUserInfo(oauthToken: String): UserInfo
}
