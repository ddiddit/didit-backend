package com.didit.adapter.retrospect.`in`.web.response

import com.didit.domain.retrospect.enums.QuestionType
import java.util.UUID

data class StartRetrospectiveResponse(
    val retrospectiveId: UUID,
    val questionType: QuestionType,
    val question: String
)