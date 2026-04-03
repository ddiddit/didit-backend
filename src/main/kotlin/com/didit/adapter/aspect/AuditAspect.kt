package com.didit.adapter.aspect

import com.didit.application.audit.ActorType
import com.didit.application.audit.Audit
import com.didit.application.audit.AuditLogger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.UUID

@Aspect
@Component
class AuditAspect(
    private val auditLogger: AuditLogger,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AuditAspect::class.java)
    }

    @Around("@annotation(audit)")
    fun logAudit(
        joinPoint: ProceedingJoinPoint,
        audit: Audit,
    ): Any? {
        val result = joinPoint.proceed()
        runCatching { log(joinPoint, audit) }
            .onFailure { logger.error("audit 로그 저장 실패 - action: ${audit.action}", it) }
        return result
    }

    private fun log(
        joinPoint: ProceedingJoinPoint,
        audit: Audit,
    ) {
        val actorId = extractActorId() ?: return
        val actorType = extractActorType()

        auditLogger.log(
            actorId = actorId,
            actorType = actorType,
            action = audit.action,
            targetId = joinPoint.uuidArgs().targetId(audit),
            targetType = audit.targetType.ifBlank { null },
        )
    }

    private fun extractActorId(): UUID? =
        runCatching {
            UUID.fromString(SecurityContextHolder.getContext().authentication?.name)
        }.getOrNull()

    private fun extractActorType(): ActorType {
        val authorities = SecurityContextHolder.getContext().authentication?.authorities
        return if (authorities?.any { it.authority.startsWith("ROLE_ADMIN") || it.authority.startsWith("ROLE_SUPER") } == true) {
            ActorType.ADMIN
        } else {
            ActorType.USER
        }
    }

    private fun ProceedingJoinPoint.uuidArgs(): List<UUID> = args.filterIsInstance<UUID>()

    private fun List<UUID>.targetId(audit: Audit): UUID? {
        if (audit.targetType.isBlank()) return null
        return drop(1).firstOrNull()
    }
}
