package com.didit.application.achievement.dto

data class MissionInfo(
    val type: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val remainingDays: Int?,
    val cta: String,
)
