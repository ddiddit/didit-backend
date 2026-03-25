package com.didit.application.auth.provided

import com.didit.domain.auth.Job
import java.util.UUID

interface UserRegister {
    fun register(
        userId: UUID,
        nickname: String,
        job: Job,
        marketingAgreed: Boolean,
    )

    fun updateProfile(
        userId: UUID,
        nickname: String,
        job: Job,
    )
}
