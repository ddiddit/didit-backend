// RetrospectiveRepository.kt
package com.didit.application.retrospect.required

import com.didit.domain.retrospect.RetroStatus
import com.didit.domain.retrospect.Retrospective
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.Repository
import java.time.LocalDateTime
import java.util.UUID

interface RetrospectiveRepository : Repository<Retrospective, UUID> {
    fun save(retrospective: Retrospective): Retrospective

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Retrospective?

    fun findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId: UUID): List<Retrospective>

    fun findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        userId: UUID,
        pageable: Pageable,
    ): List<Retrospective>

    fun findFirstByUserIdAndStatusAndDeletedAtIsNull(
        userId: UUID,
        status: RetroStatus,
    ): Retrospective?

    fun countByUserIdAndStatusNotAndCreatedAtBetween(
        userId: UUID,
        status: RetroStatus,
        from: LocalDateTime,
        to: LocalDateTime,
    ): Int

    fun findByUserIdAndDeletedAtIsNullAndCreatedAtBetweenOrderByCreatedAtDesc(
        userId: UUID,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<Retrospective>
}
