package com.didit.adapter.webapi.auth.dto

import com.didit.domain.auth.Job
import com.didit.domain.auth.Provider
import com.didit.domain.auth.User

data class UserProfileResponse(
    val nickname: String?,
    val job: Job?,
    val email: String?,
    val provider: Provider,
) {
    companion object {
        fun from(user: User) =
            UserProfileResponse(
                nickname = user.nickname,
                job = user.job,
                email = user.email,
                provider = user.provider,
            )
    }
}
