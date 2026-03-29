package com.didit.application.retrospect

import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.Retrospective
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrospectQueryService(
    private val retrospectiveRepository: RetrospectiveRepository,
) : RetrospectiveFinder {
    override fun findById(
        retrospectiveId: UUID,
        userId: UUID,
    ): Retrospective =
        retrospectiveRepository.findByIdAndUserId(retrospectiveId, userId)
            ?: throw RetrospectiveNotFoundException(retrospectiveId)

    override fun findAllByUserId(userId: UUID): List<Retrospective> =
        retrospectiveRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)

    override fun countByUserIdAndDate(
        userId: UUID,
        date: LocalDate,
    ): Int =
        retrospectiveRepository.countByUserIdAndCreatedAtBetweenAndDeletedAtIsNull(
            userId = userId,
            from = date.atStartOfDay(),
            to = date.atTime(23, 59, 59),
        )
}
