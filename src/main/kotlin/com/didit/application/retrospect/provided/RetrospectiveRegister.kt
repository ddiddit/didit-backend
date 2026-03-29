package com.didit.application.retrospect.provided

import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import com.didit.domain.retrospect.Retrospective
import java.util.UUID

interface RetrospectiveRegister {
    fun start(userId: UUID): Retrospective

    fun submitAnswer(
        retrospectiveId: UUID,
        userId: UUID,
        content: String,
    ): SubmitAnswerResponse

    fun skipDeepQuestion(
        retrospectiveId: UUID,
        userId: UUID,
    )

    fun complete(
        retrospectiveId: UUID,
        userId: UUID,
    ): AISummaryResponse

    fun save(
        retrospectiveId: UUID,
        userId: UUID,
        title: String,
        projectId: UUID?,
        summary: AISummaryResponse,
    ): Retrospective

    fun restart(
        retrospectiveId: UUID,
        userId: UUID,
    ): Retrospective

    fun delete(
        retrospectiveId: UUID,
        userId: UUID,
    )
}
