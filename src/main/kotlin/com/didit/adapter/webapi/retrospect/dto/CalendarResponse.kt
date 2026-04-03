package com.didit.adapter.webapi.retrospect.dto

import com.didit.domain.retrospect.Retrospective
import java.time.LocalDate

data class CalendarResponse(
    val year: Int,
    val month: Int,
    val days: List<CalendarDayResponse>,
    val weeklyCount: Int,
    val isWeeklyGoalAchieved: Boolean,
) {
    data class CalendarDayResponse(
        val date: LocalDate,
        val count: Int,
    )

    companion object {
        private const val WEEKLY_GOAL = 3

        fun of(
            year: Int,
            month: Int,
            retrospectives: List<Retrospective>,
            weeklyRetrospectives: List<Retrospective>,
        ): CalendarResponse {
            val days =
                retrospectives
                    .filter { it.completedAt != null }
                    .groupBy { it.completedAt!!.toLocalDate() }
                    .map { (date, retros) -> CalendarDayResponse(date = date, count = retros.size) }
                    .sortedBy { it.date }

            val weeklyCount = weeklyRetrospectives.size

            return CalendarResponse(
                year = year,
                month = month,
                days = days,
                weeklyCount = weeklyCount,
                isWeeklyGoalAchieved = weeklyCount >= WEEKLY_GOAL,
            )
        }
    }
}
