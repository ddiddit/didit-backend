package com.didit.adapter.webapi.retrospect.dto

import com.didit.domain.retrospect.InputType

data class SubmitAnswerRequest(
    val content: String,
    val inputType: InputType,
)
