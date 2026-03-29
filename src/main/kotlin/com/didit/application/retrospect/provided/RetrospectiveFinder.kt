package com.didit.application.retrospect.provided

import com.didit.domain.retrospect.Retrospective
import java.time.LocalDate
import java.util.UUID

interface RetrospectiveFinder {
    fun findById(
        retrospectiveId: UUID,
        userId: UUID,
    ): Retrospective

    fun findAllByUserId(userId: UUID): List<Retrospective>

    fun countByUserIdAndDate(
        userId: UUID,
        date: LocalDate,
    ): Int
}
