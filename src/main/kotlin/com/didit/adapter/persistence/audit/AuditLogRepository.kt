package com.didit.adapter.persistence.audit

import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface AuditLogRepository : Repository<AuditLog, UUID> {
    fun save(auditLogEntity: AuditLog): AuditLog

    fun findTopByActorIdAndActionOrderByCreatedAtDesc(
        actorId: UUID,
        action: AuditAction,
    ): AuditLog?

    fun findTop20ByActorIdAndActionInOrderByCreatedAtDesc(
        actorId: UUID,
        actions: List<AuditAction>,
    ): List<AuditLog>

    @Query(
        """
        SELECT a.actorId as userId, MAX(a.createdAt) as lastLoginAt
        FROM AuditLog a
        WHERE a.actorId IN :userIds AND a.action = 'USER_LOGGED_IN'
        GROUP BY a.actorId
        """,
    )
    fun findLastLoginByUserIds(
        @Param("userIds") userIds: List<UUID>,
    ): List<LastLoginProjection>

    @Query(
        "SELECT COUNT(DISTINCT a.actorId) FROM AuditLog a " +
            "WHERE a.action = :action AND a.createdAt >= :since AND a.actorType = :actorType",
    )
    fun countDistinctActorSince(
        @Param("action") action: AuditAction,
        @Param("since") since: LocalDateTime,
        @Param("actorType") actorType: ActorType,
    ): Long

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<AuditLog>

    @Query(
        "SELECT a FROM AuditLog a " +
            "WHERE (:action IS NULL OR a.action = :action) " +
            "AND (:actorType IS NULL OR a.actorType = :actorType) " +
            "ORDER BY a.createdAt DESC",
    )
    fun findFiltered(
        @Param("action") action: AuditAction?,
        @Param("actorType") actorType: ActorType?,
        pageable: Pageable,
    ): Page<AuditLog>
}
