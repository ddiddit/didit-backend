package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.provided.AdminRetrospectiveStatsFinder
import com.didit.application.admin.provided.AdminRetrospectiveStatsResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/admin/retrospective-stats")
@RestController
class AdminRetrospectiveStatsApi(
    private val adminRetrospectiveStatsFinder: AdminRetrospectiveStatsFinder,
) {
    @RequireAdmin
    @GetMapping
    fun getRetrospectiveStats(): SuccessResponse<AdminRetrospectiveStatsResult> =
        SuccessResponse.of(adminRetrospectiveStatsFinder.getRetrospectiveStats())
}
