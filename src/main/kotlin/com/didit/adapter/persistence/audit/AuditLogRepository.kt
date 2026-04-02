package com.didit.adapter.persistence.audit

import org.springframework.data.repository.Repository
import java.util.UUID

interface AuditLogRepository : Repository<AuditLog, UUID> {
    fun save(auditLogEntity: AuditLog): AuditLog
}
