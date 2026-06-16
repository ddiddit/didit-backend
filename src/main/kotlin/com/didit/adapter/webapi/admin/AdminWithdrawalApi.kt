package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.provided.AdminWithdrawalStatsResult
import com.didit.application.admin.provided.AdminWithdrawalStatsFinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/admin/withdrawal-stats")
@RestController
class AdminWithdrawalApi(
    private val adminWithdrawalStatsFinder: AdminWithdrawalStatsFinder,
) {
    @RequireAdmin
    @GetMapping
    fun getWithdrawalStats(): SuccessResponse<AdminWithdrawalStatsResult> =
        SuccessResponse.of(adminWithdrawalStatsFinder.getWithdrawalStats())
}
