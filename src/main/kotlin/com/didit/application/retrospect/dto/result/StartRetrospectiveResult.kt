package com.didit.application.retrospect.dto.result

import com.didit.domain.retrospect.enums.QuestionType
import java.util.UUID

data class StartRetrospectiveResult(
    val retrospectiveId: UUID,
    val questionType: QuestionType,
    val question: String
)