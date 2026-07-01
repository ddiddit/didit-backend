package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.provided.AdminAchievementStatsFinder
import com.didit.application.admin.provided.AdminBadgeStat
import com.didit.application.admin.provided.AdminLevelStat
import com.didit.application.admin.provided.AdminMissionStat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/admin/stats")
@RestController
class AdminAchievementStatsApi(
    private val adminAchievementStatsFinder: AdminAchievementStatsFinder,
) {
    @RequireAdmin
    @GetMapping("/levels")
    fun getLevelStats(): SuccessResponse<List<AdminLevelStat>> = SuccessResponse.of(adminAchievementStatsFinder.getLevelStats())

    @RequireAdmin
    @GetMapping("/missions")
    fun getMissionStats(): SuccessResponse<List<AdminMissionStat>> = SuccessResponse.of(adminAchievementStatsFinder.getMissionStats())

    @RequireAdmin
    @GetMapping("/badges")
    fun getBadgeStats(): SuccessResponse<List<AdminBadgeStat>> = SuccessResponse.of(adminAchievementStatsFinder.getBadgeStats())
}
