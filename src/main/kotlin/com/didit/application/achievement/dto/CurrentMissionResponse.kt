package com.didit.application.achievement.dto

data class CurrentMissionResponse(
    val currentLevel: Int,
    val mission: MissionInfo?,
    val weeklyStatus: WeeklyStatus?,
    val popup: PopupStatus,
)
