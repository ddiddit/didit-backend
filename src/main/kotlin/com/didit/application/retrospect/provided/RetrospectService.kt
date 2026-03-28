package com.didit.application.retrospect.provided

import com.didit.application.retrospect.dto.StartRetrospectiveResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import java.util.UUID

interface RetrospectService {
    fun startRetrospective(userId: UUID): StartRetrospectiveResponse

    fun submitAnswer(
        userId: UUID,
        answer: String,
    ): SubmitAnswerResponse
}
