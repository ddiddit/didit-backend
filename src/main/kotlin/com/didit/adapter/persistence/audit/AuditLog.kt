package com.didit.adapter.persistence.audit

import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "audit_logs")
@Entity
class AuditLog(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val actorId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val actorType: ActorType,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val action: AuditAction,
    @Column(columnDefinition = "BINARY(16)")
    val targetId: UUID? = null,
    @Column(length = 50)
    val targetType: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    val payload: Map<String, Any>? = null,
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
