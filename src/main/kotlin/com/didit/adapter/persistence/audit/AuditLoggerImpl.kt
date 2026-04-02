package com.didit.adapter.persistence.audit

import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AuditLoggerImpl(
    private val auditLogJpaRepository: AuditLogRepository,
) : AuditLogger {
    @Async
    override fun log(
        actorId: UUID,
        actorType: ActorType,
        action: AuditAction,
        targetId: UUID?,
        targetType: String?,
        payload: Map<String, Any>?,
    ) {
        auditLogJpaRepository.save(
            AuditLog(
                actorId = actorId,
                actorType = actorType,
                action = action,
                targetId = targetId,
                targetType = targetType,
                payload = payload,
            ),
        )
    }
}
