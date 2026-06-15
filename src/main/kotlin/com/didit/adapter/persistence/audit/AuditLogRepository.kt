package com.didit.adapter.persistence.audit

import com.didit.application.audit.AuditAction
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
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
}
