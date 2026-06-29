package com.didit.application.achievement.dto

import java.time.LocalDate

data class WeeklyStatus(
    val show: Boolean,
    val weekStart: LocalDate,
    val days: List<Boolean>,
)
