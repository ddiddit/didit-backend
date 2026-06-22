package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.provided.AdminAuditActionItem
import com.didit.application.admin.provided.AdminAuditFinder
import com.didit.application.admin.provided.AdminAuditLogsResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/admin/audit-logs")
@RestController
class AdminAuditApi(
    private val adminAuditFinder: AdminAuditFinder,
) {
    @RequireAdmin
    @GetMapping
    fun findAuditLogs(
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) actorType: String?,
        @RequestParam(defaultValue = "0") page: Int,
    ): SuccessResponse<AdminAuditLogsResult> = SuccessResponse.of(adminAuditFinder.findAuditLogs(action, actorType, page))

    @RequireAdmin
    @GetMapping("/actions")
    fun findAuditActions(): SuccessResponse<List<AdminAuditActionItem>> = SuccessResponse.of(adminAuditFinder.findAuditActions())
}
