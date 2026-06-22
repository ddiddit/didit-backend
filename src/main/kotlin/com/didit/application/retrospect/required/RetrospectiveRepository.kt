package com.didit.application.retrospect.required

import com.didit.domain.retrospect.InputType
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

    @Query(
        """
        SELECT COUNT(r) FROM Retrospective r
        WHERE r.userId = :userId
        AND r.status <> :status
        AND r.deletedAt IS NULL
        AND r.createdAt >= :from AND r.createdAt < :to
    """,
    )
    fun countByUserIdAndCreatedAtInPeriod(
        @Param("userId") userId: UUID,
        @Param("status") status: RetroStatus,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
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
        AND r.completedAt >= :from AND r.completedAt < :to
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

    fun findAllByIdInAndDeletedAtIsNull(ids: List<UUID>): List<Retrospective>

    fun countByStatusAndDeletedAtIsNull(status: RetroStatus): Long

    fun countByCompletedAtBetweenAndDeletedAtIsNull(
        from: LocalDateTime,
        to: LocalDateTime,
    ): Long

    fun countByCreatedAtBetweenAndDeletedAtIsNull(
        from: LocalDateTime,
        to: LocalDateTime,
    ): Long

    fun countByCreatedAtBetweenAndStatusAndDeletedAtIsNull(
        from: LocalDateTime,
        to: LocalDateTime,
        status: RetroStatus,
    ): Long

    @Query(
        nativeQuery = true,
        value = """
            SELECT DATE(completed_at) as `date`, COUNT(*) as `count`
            FROM retrospectives
            WHERE completed_at >= :since AND deleted_at IS NULL AND status = 'COMPLETED'
            GROUP BY DATE(completed_at)
            ORDER BY DATE(completed_at)
        """,
    )
    fun findWeeklyRetroTrend(
        @Param("since") since: LocalDateTime,
    ): List<DailyRetroProjection>

    interface DailyRetroProjection {
        fun getDate(): java.sql.Date

        fun getCount(): Long
    }

    @Query(
        """
        SELECT r
        FROM Retrospective r
        JOIN RetrospectiveTag rt ON rt.retrospectiveId = r.id
        WHERE rt.tagId = :tagId
        AND r.deletedAt IS NULL
        AND rt.deletedAt IS NULL
    """,
    )
    fun findAllByTagId(tagId: UUID): List<Retrospective>

    @Query("SELECT COALESCE(SUM(r.inputTokens), 0) FROM Retrospective r WHERE r.deletedAt IS NULL")
    fun sumInputTokens(): Long

    @Query("SELECT COALESCE(SUM(r.outputTokens), 0) FROM Retrospective r WHERE r.deletedAt IS NULL")
    fun sumOutputTokens(): Long

    @Query("SELECT COALESCE(SUM(r.inputTokens), 0) FROM Retrospective r WHERE r.deletedAt IS NULL AND r.completedAt BETWEEN :from AND :to")
    fun sumInputTokensByCompletedAtBetween(
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): Long

    @Query("SELECT COALESCE(SUM(r.outputTokens), 0) FROM Retrospective r WHERE r.deletedAt IS NULL AND r.completedAt BETWEEN :from AND :to")
    fun sumOutputTokensByCompletedAtBetween(
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): Long

    @Query(
        """
        SELECT COUNT(m)
        FROM ChatMessage m
        WHERE m.sender = com.didit.domain.retrospect.Sender.USER
        AND m.inputType = :inputType
    """,
    )
    fun countUserAnswersByInputType(
        @Param("inputType") inputType: InputType,
    ): Long
}
