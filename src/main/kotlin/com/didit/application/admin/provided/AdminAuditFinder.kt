package com.didit.application.admin.provided

interface AdminAuditFinder {
    fun findAuditLogs(action: String?, actorType: String?, page: Int): AdminAuditLogsResult
}
