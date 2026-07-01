package com.didit.application.admin.provided

interface AdminAchievementStatsFinder {
    fun getLevelStats(): List<AdminLevelStat>

    fun getMissionStats(): List<AdminMissionStat>

    fun getBadgeStats(): List<AdminBadgeStat>
}
