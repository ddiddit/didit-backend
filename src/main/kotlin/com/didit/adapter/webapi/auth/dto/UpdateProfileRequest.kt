package com.didit.adapter.webapi.auth.dto

import com.didit.domain.auth.Job

data class UpdateProfileRequest(
    val nickname: String,
    val job: Job,
)
