package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.CurrentAdminId
import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.provided.AdminBadgeCreateCommand
import com.didit.application.admin.provided.AdminBadgeFinder
import com.didit.application.admin.provided.AdminBadgeHolder
import com.didit.application.admin.provided.AdminBadgeMetaResult
import com.didit.application.admin.provided.AdminBadgeRegister
import com.didit.application.admin.provided.AdminBadgeResult
import com.didit.application.admin.provided.AdminBadgeUpdateCommand
import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/admin/badges")
@RestController
class AdminBadgeApi(
    private val adminBadgeFinder: AdminBadgeFinder,
    private val adminBadgeRegister: AdminBadgeRegister,
    private val auditLogger: AuditLogger,
) {
    @RequireAdmin
    @GetMapping
    fun findAll(): SuccessResponse<List<AdminBadgeResult>> = SuccessResponse.of(adminBadgeFinder.findAll())

    @RequireAdmin
    @GetMapping("/condition-types")
    fun findMeta(): SuccessResponse<AdminBadgeMetaResult> = SuccessResponse.of(adminBadgeFinder.findMeta())

    @RequireAdmin
    @GetMapping("/{badgeId}")
    fun findById(
        @PathVariable badgeId: UUID,
    ): SuccessResponse<AdminBadgeResult> = SuccessResponse.of(adminBadgeFinder.findById(badgeId))

    @RequireAdmin
    @GetMapping("/{badgeId}/holders")
    fun findHolders(
        @PathVariable badgeId: UUID,
    ): SuccessResponse<List<AdminBadgeHolder>> = SuccessResponse.of(adminBadgeFinder.findHolders(badgeId))

    @RequireAdmin
    @PostMapping
    fun create(
        @RequestBody request: AdminBadgeCreateRequest,
        @CurrentAdminId adminId: UUID,
    ): SuccessResponse<AdminBadgeResult> {
        val result = adminBadgeRegister.create(request.toCommand())
        auditLogger.log(
            actorId = adminId,
            actorType = ActorType.ADMIN,
            action = AuditAction.ADMIN_BADGE_CREATED,
            targetId = result.id,
            targetType = "BADGE",
        )
        return SuccessResponse.of(result)
    }

    @RequireAdmin
    @PutMapping("/{badgeId}")
    fun update(
        @PathVariable badgeId: UUID,
        @RequestBody request: AdminBadgeUpdateRequest,
        @CurrentAdminId adminId: UUID,
    ): SuccessResponse<AdminBadgeResult> {
        val result = adminBadgeRegister.update(badgeId, request.toCommand())
        auditLogger.log(
            actorId = adminId,
            actorType = ActorType.ADMIN,
            action = AuditAction.ADMIN_BADGE_UPDATED,
            targetId = badgeId,
            targetType = "BADGE",
        )
        return SuccessResponse.of(result)
    }

    @RequireAdmin
    @PatchMapping("/{badgeId}/active")
    fun changeActive(
        @PathVariable badgeId: UUID,
        @RequestBody request: AdminBadgeActiveRequest,
        @CurrentAdminId adminId: UUID,
    ): SuccessResponse<AdminBadgeResult> {
        val result = adminBadgeRegister.changeActive(badgeId, request.active)
        auditLogger.log(
            actorId = adminId,
            actorType = ActorType.ADMIN,
            action = AuditAction.ADMIN_BADGE_ACTIVE_CHANGED,
            targetId = badgeId,
            targetType = "BADGE",
            payload = mapOf("active" to request.active),
        )
        return SuccessResponse.of(result)
    }
}

data class AdminBadgeCreateRequest(
    val name: String,
    val description: String,
    val category: String,
    val conditionType: String,
    val threshold: Int,
    val params: Map<String, Any>? = null,
    val iconUrl: String? = null,
    val congratsTitle: String? = null,
    val congratsMessage: String? = null,
) {
    fun toCommand() =
        AdminBadgeCreateCommand(
            name = name,
            description = description,
            category = category,
            conditionType = conditionType,
            threshold = threshold,
            params = params,
            iconUrl = iconUrl,
            congratsTitle = congratsTitle,
            congratsMessage = congratsMessage,
        )
}

data class AdminBadgeUpdateRequest(
    val name: String,
    val description: String,
    val category: String,
    val conditionType: String,
    val threshold: Int,
    val params: Map<String, Any>? = null,
    val iconUrl: String? = null,
    val congratsTitle: String? = null,
    val congratsMessage: String? = null,
) {
    fun toCommand() =
        AdminBadgeUpdateCommand(
            name = name,
            description = description,
            category = category,
            conditionType = conditionType,
            threshold = threshold,
            params = params,
            iconUrl = iconUrl,
            congratsTitle = congratsTitle,
            congratsMessage = congratsMessage,
        )
}

data class AdminBadgeActiveRequest(
    val active: Boolean,
)
