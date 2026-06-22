package com.didit.application.retrospect.provided

import com.didit.application.retrospect.dto.DeepQuestionResponse
import com.didit.application.retrospect.dto.RetrospectiveDetailResult
import com.didit.domain.retrospect.Retrospective
import java.time.LocalDate
import java.util.UUID

interface RetrospectiveFinder {
    fun findById(
        retrospectiveId: UUID,
        userId: UUID,
    ): Retrospective

    fun findAllByUserId(userId: UUID): List<Retrospective>

    fun findRecentByUserId(
        userId: UUID,
        limit: Int,
    ): List<Retrospective>

    fun countByUserIdAndDate(
        userId: UUID,
        date: LocalDate,
    ): Int

    fun findByUserIdAndYearMonth(
        userId: UUID,
        year: Int,
        month: Int,
    ): List<Retrospective>

    fun findByUserIdAndDate(
        userId: UUID,
        date: LocalDate,
    ): List<Retrospective>

    fun findByUserIdAndCurrentWeek(userId: UUID): List<Retrospective>

    fun findDeepQuestion(
        retrospectiveId: UUID,
        userId: UUID,
    ): DeepQuestionResponse

    fun searchByTitle(
        userId: UUID,
        keyword: String,
    ): List<Retrospective>

    fun findByProject(
        userId: UUID,
        projectId: UUID,
    ): List<Retrospective>

    fun findRetrospectWithProjectAndTags(
        retrospectiveId: UUID,
        userId: UUID,
    ): RetrospectiveDetailResult
}
