package com.didit.application.audit

import java.util.UUID

interface AuditLogger {
    fun log(
        actorId: UUID,
        actorType: ActorType,
        action: AuditAction,
        targetId: UUID? = null,
        targetType: String? = null,
        payload: Map<String, Any>? = null,
    )
}
