package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.provided.AdminBadgeFinder
import com.didit.application.admin.provided.AdminBadgeHolder
import com.didit.application.admin.provided.AdminBadgeResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/admin/badges")
@RestController
class AdminBadgeApi(
    private val adminBadgeFinder: AdminBadgeFinder,
) {
    @RequireAdmin
    @GetMapping
    fun findAll(): SuccessResponse<List<AdminBadgeResult>> = SuccessResponse.of(adminBadgeFinder.findAll())

    @RequireAdmin
    @GetMapping("/{badgeId}/holders")
    fun findHolders(
        @PathVariable badgeId: UUID,
    ): SuccessResponse<List<AdminBadgeHolder>> = SuccessResponse.of(adminBadgeFinder.findHolders(badgeId))
}
