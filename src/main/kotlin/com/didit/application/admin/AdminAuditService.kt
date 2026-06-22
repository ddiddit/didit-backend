package com.didit.application.admin

import com.didit.application.admin.provided.AdminAuditActionItem
import com.didit.application.admin.provided.AdminAuditFinder
import com.didit.application.admin.provided.AdminAuditLogItem
import com.didit.application.admin.provided.AdminAuditLogsResult
import com.didit.application.audit.AdminAuditLogEntry
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class AdminAuditService(
    private val auditReader: AuditReader,
) : AdminAuditFinder {
    override fun findAuditLogs(
        action: String?,
        actorType: String?,
        page: Int,
    ): AdminAuditLogsResult {
        val auditAction = resolveAuditAction(action)
        val result = auditReader.findAuditLogs(action = auditAction, actorType = actorType, page = page, size = 20)
        return AdminAuditLogsResult(
            content = result.content.map { it.toAdminItem() },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.page,
        )
    }

    override fun findAuditActions(): List<AdminAuditActionItem> =
        AuditAction.entries.map {
            AdminAuditActionItem(
                action = it.name,
                label = it.label,
                actorType = it.actorType.name,
            )
        }

    private fun resolveAuditAction(action: String?): AuditAction? =
        action?.let {
            AuditAction.entries.find { a -> a.name == it }
                ?: throw IllegalArgumentException("유효하지 않은 action 값: $it")
        }

    private fun AdminAuditLogEntry.toAdminItem() =
        AdminAuditLogItem(
            action = action.name,
            actionLabel = action.label,
            actorId = actorId,
            actorType = actorType,
            targetId = targetId,
            targetType = targetType,
            payload = payload,
            createdAt = createdAt,
        )
}
