package com.didit.adapter.retrospect.out.ai.dto

data class ClovaStudioChatResponse(
    val status: ClovaStudioStatusDto? = null,
    val result: ClovaStudioResultDto? = null
)

data class ClovaStudioStatusDto(
    val code: String? = null,
    val message: String? = null
)

data class ClovaStudioResultDto(
    val message: ClovaStudioResultMessageDto? = null,
    val inputLength: Int? = null,
    val outputLength: Int? = null
)

data class ClovaStudioResultMessageDto(
    val role: String? = null,
    val content: String? = null
)