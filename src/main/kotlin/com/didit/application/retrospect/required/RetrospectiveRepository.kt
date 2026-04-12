package com.didit.application.retrospect.required

import com.didit.domain.retrospect.RetroStatus
import com.didit.domain.retrospect.Retrospective
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface RetrospectiveRepository : Repository<Retrospective, UUID> {
    fun save(retrospective: Retrospective): Retrospective

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Retrospective?

    fun findByIdAndDeletedAtIsNull(retrospectiveId: UUID): Retrospective?

    fun countByUserIdAndStatusNotAndCreatedAtBetween(
        userId: UUID,
        status: RetroStatus,
        from: LocalDateTime,
        to: LocalDateTime,
    ): Int

    fun countByUserIdAndStatusAndDeletedAtIsNull(
        userId: UUID,
        status: RetroStatus,
    ): Int

    @Query(
        """
        SELECT r FROM Retrospective r
        WHERE r.userId = :userId
        AND r.status = 'COMPLETED'
        AND r.deletedAt IS NULL
        ORDER BY r.createdAt DESC
    """,
    )
    fun findAllCompletedByUserId(
        @Param("userId") userId: UUID,
    ): List<Retrospective>

    @Query(
        """
        SELECT r FROM Retrospective r
        WHERE r.userId = :userId
        AND r.status = 'COMPLETED'
        AND r.deletedAt IS NULL
        ORDER BY r.createdAt DESC
    """,
    )
    fun findRecentCompletedByUserId(
        @Param("userId") userId: UUID,
        pageable: Pageable,
    ): List<Retrospective>

    @Query(
        """
        SELECT r FROM Retrospective r
        WHERE r.userId = :userId
        AND r.status = 'COMPLETED'
        AND r.deletedAt IS NULL
        AND r.completedAt BETWEEN :from AND :to
        ORDER BY r.completedAt DESC
    """,
    )
    fun findCompletedByUserIdAndPeriod(
        @Param("userId") userId: UUID,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<Retrospective>

    @Query(
        """
        SELECT r FROM Retrospective r
        WHERE r.deletedAt IS NULL
        AND r.userId = :userId
        AND LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY r.createdAt DESC
    """,
    )
    fun searchByUserIdAndTitle(
        @Param("userId") userId: UUID,
        @Param("keyword") keyword: String,
    ): List<Retrospective>

    @Query(
        "SELECT r.completedAt FROM Retrospective r " +
            "WHERE r.userId = :userId AND r.status = :status AND r.deletedAt IS NULL",
    )
    fun findCompletedAtByUserIdAndStatusAndDeletedAtIsNull(
        @Param("userId") userId: UUID,
        @Param("status") status: RetroStatus,
    ): List<LocalDateTime>

    fun findByIdAndUserIdAndDeletedAtIsNull(
        retrospectiveId: UUID,
        userId: UUID,
    ): Retrospective?

    fun findAllByProjectIdAndDeletedAtIsNull(projectId: UUID): List<Retrospective>

    @Query(
        """
            SELECT r FROM Retrospective r
            WHERE r.userId=:userId AND r.status = 'COMPLETED'
            AND r.projectId=:projectId
            AND r.deletedAt IS NULL
            ORDER BY r.createdAt DESC
        """,
    )
    fun findAllByUserIdAndProjectId(
        @Param("userId") userId: UUID,
        @Param("projectId") projectId: UUID,
    ): List<Retrospective>

    @Query(
        """
            SELECT r FROM Retrospective r
            WHERE r.status = 'PENDING'
            AND r.deletedAt IS NULL
            AND r.createdAt < :cutoff
        """,
    )
    fun findAllPendingBefore(
        @Param("cutoff") cutoff: LocalDateTime,
    ): List<Retrospective>

    fun delete(retrospective: Retrospective)

    fun findAllByUserId(userId: UUID): List<Retrospective>
}
