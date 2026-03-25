package com.didit.adapter.webapi.auth.dto

import com.didit.domain.auth.Job

data class OnboardingRequest(
    val nickname: String,
    val job: Job,
    val marketingAgreed: Boolean,
    val nightPushAgreed: Boolean,
)
