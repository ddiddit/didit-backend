package com.didit.adapter.webapi.retrospect.dto

import com.didit.domain.retrospect.Retrospective
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

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
        ): CalendarResponse {
            val days =
                retrospectives
                    .filter { it.createdAt != null }
                    .groupBy { it.createdAt!!.toLocalDate() }
                    .map { (date, retros) -> CalendarDayResponse(date = date, count = retros.size) }
                    .sortedBy { it.date }

            val today = LocalDate.now()
            val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

            val weeklyCount =
                retrospectives
                    .filter { it.createdAt != null }
                    .count {
                        val date = it.createdAt!!.toLocalDate()
                        !date.isBefore(weekStart) && !date.isAfter(weekEnd)
                    }

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
