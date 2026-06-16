package com.didit.application.admin

import com.didit.application.admin.provided.AdminAuditFinder
import com.didit.application.admin.provided.AdminAuditLogItem
import com.didit.application.admin.provided.AdminAuditLogsResult
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
        val auditAction =
            action?.let {
                AuditAction.entries.find { a -> a.name == it }
                    ?: throw IllegalArgumentException("유효하지 않은 action 값: $it")
            }
        val result = auditReader.findAuditLogs(action = auditAction, actorType = actorType, page = page, size = 20)

        return AdminAuditLogsResult(
            content =
                result.content.map {
                    AdminAuditLogItem(
                        action = it.action.name,
                        actorId = it.actorId,
                        actorType = it.actorType,
                        targetId = it.targetId,
                        targetType = it.targetType,
                        payload = it.payload,
                        createdAt = it.createdAt,
                    )
                },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.page,
        )
    }
}
