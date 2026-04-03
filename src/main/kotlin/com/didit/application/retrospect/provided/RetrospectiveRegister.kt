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

    fun submitVoiceAnswer(
        retrospectiveId: UUID,
        userId: UUID,
        audioBytes: ByteArray,
        filename: String,
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
    ): Retrospective

    fun restart(
        retrospectiveId: UUID,
        userId: UUID,
    ): Retrospective

    fun updateTitle(
        retrospectiveId: UUID,
        userId: UUID,
        title: String,
    )

    fun delete(
        retrospectiveId: UUID,
        userId: UUID,
    )

    fun exit(
        retrospectiveId: UUID,
        userId: UUID,
    )

    fun assignProject(
        userId: UUID,
        retrospectiveId: UUID,
        projectId: UUID,
    )
}
