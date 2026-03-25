package com.didit.adapter.webapi.auth.dto

import com.didit.domain.auth.Job
import jakarta.validation.constraints.Pattern

data class OnboardingRequest(
    @field:Pattern(
        regexp = "^[가-힣a-zA-Z0-9]{2,10}$",
        message = "닉네임은 2~10자 한글, 영문, 숫자만 가능합니다.",
    )
    val nickname: String,
    val job: Job,
    val marketingAgreed: Boolean,
    val nightPushAgreed: Boolean,
)
