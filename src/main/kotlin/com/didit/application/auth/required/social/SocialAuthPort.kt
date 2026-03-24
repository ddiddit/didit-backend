package com.didit.application.auth.required.social

import com.didit.domain.auth.enums.SocialProvider
import com.didit.domain.auth.model.SocialUserInfo

interface SocialAuthPort {
    fun verifyIdToken(
        provider: SocialProvider,
        idToken: String,
    ): SocialUserInfo
}
