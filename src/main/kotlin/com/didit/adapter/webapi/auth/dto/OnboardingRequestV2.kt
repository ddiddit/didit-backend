package com.didit.adapter.webapi.auth.dto

import com.didit.domain.auth.UserAge
import com.didit.domain.auth.UserExperience
import com.didit.domain.shared.Job
import jakarta.validation.constraints.Pattern

data class OnboardingRequestV2(
    @field:Pattern(
        regexp = "^[가-힣a-zA-Z]{2,10}$",
        message = "닉네임은 2~10자 한글, 영문만 가능합니다.",
    )
    val nickname: String,
    val job: Job,
    val age: UserAge,
    val experience: UserExperience,
    val marketingAgreed: Boolean,
    val nightPushAgreed: Boolean,
)
