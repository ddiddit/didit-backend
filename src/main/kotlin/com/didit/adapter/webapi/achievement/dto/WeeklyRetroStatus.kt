package com.didit.adapter.webapi.achievement.dto

data class WeeklyRetroStatus(
    val weekDays: List<WeekDayStatus>,
) {
    companion object {
        fun empty(): WeeklyRetroStatus =
            WeeklyRetroStatus(
                weekDays =
                    listOf(
                        WeekDayStatus("월", false),
                        WeekDayStatus("화", false),
                        WeekDayStatus("수", false),
                        WeekDayStatus("목", false),
                        WeekDayStatus("금", false),
                        WeekDayStatus("토", false),
                        WeekDayStatus("일", false),
                    ),
            )
    }
}
