package com.didit.adapter.retrospect.inbound.web.response

import com.didit.domain.retrospect.enums.QuestionType

data class SubmitAnswerResponse(
    val completed: Boolean,
    val questionType: QuestionType? = null,
    val question: String? = null,
    val summary: RetrospectiveSummaryResponse? = null,
)
