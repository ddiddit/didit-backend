package com.didit.domain.auth.model

import com.didit.domain.auth.enums.SocialProvider

data class SocialUserInfo(
    val socialId: String,
    val email: String?,
    val provider: SocialProvider,
)
