package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.provided.AdminAnalyticsFinder
import com.didit.application.admin.provided.AdminAnalyticsResult
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admin/analytics")
class AdminAnalyticsApi(
    private val finder: AdminAnalyticsFinder,
) {
    @RequireAdmin
    @GetMapping
    fun find(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): SuccessResponse<AdminAnalyticsResult> = SuccessResponse.of(finder.find(from, to))
}
