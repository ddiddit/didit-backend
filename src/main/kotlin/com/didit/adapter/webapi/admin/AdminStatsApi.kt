package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.provided.AdminStatsFinder
import com.didit.application.admin.provided.AdminStatsResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/admin/stats")
@RestController
class AdminStatsApi(
    private val adminStatsFinder: AdminStatsFinder,
) {
    @RequireAdmin
    @GetMapping
    fun getStats(): SuccessResponse<AdminStatsResult> = SuccessResponse.of(adminStatsFinder.getStats())
}
