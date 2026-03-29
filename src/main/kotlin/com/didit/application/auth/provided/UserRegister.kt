package com.didit.application.auth.provided

import com.didit.domain.shared.Job
import java.util.UUID

interface UserRegister {
    fun register(
        userId: UUID,
        nickname: String,
        job: Job,
        marketingAgreed: Boolean,
        nightPushAgreed: Boolean,
    )

    fun updateProfile(
        userId: UUID,
        nickname: String,
        job: Job,
    )

    fun updateMarketingConsent(
        userId: UUID,
        agreed: Boolean,
    )
}
