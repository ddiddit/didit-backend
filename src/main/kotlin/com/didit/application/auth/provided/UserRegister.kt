package com.didit.application.auth.provided

import com.didit.domain.auth.UserAge
import com.didit.domain.auth.UserExperience
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

    fun registerV2(
        userId: UUID,
        nickname: String,
        job: Job,
        age: UserAge? = null,
        experience: UserExperience? = null,
        marketingAgreed: Boolean,
        nightPushAgreed: Boolean,
    )

    fun updateProfile(
        userId: UUID,
        nickname: String,
        job: Job,
    )

    fun updateProfileV2(
        userId: UUID,
        nickname: String,
        job: Job,
        age: UserAge? = null,
        experience: UserExperience? = null,
    )

    fun updateMarketingConsent(
        userId: UUID,
        agreed: Boolean,
    )
}
