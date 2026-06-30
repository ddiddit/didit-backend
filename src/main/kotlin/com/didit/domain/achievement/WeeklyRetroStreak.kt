package com.didit.domain.achievement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "weekly_retro_streaks")
@Entity
class WeeklyRetroStreak(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(nullable = false)
    var currentWeeks: Int = 0,
    @Column(nullable = false)
    var longestWeeks: Int = 0,
    @Column
    var lastAchievedWeek: LocalDate? = null,
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun recordRetro(retroDateKst: LocalDate) {
        val weekStart = weekStartOf(retroDateKst)
        if (isSameWeek(weekStart)) return
        if (isNextWeek(weekStart)) return increaseStreak(weekStart)
        resetStreak(weekStart)
    }

    fun isStreak(weeks: Int): Boolean = currentWeeks >= weeks

    private fun isSameWeek(weekStart: LocalDate): Boolean = lastAchievedWeek == weekStart

    private fun isNextWeek(weekStart: LocalDate): Boolean = lastAchievedWeek?.plusWeeks(1) == weekStart

    private fun increaseStreak(weekStart: LocalDate) {
        currentWeeks += 1
        longestWeeks = maxOf(longestWeeks, currentWeeks)
        lastAchievedWeek = weekStart
        updatedAt = LocalDateTime.now()
    }

    private fun resetStreak(weekStart: LocalDate) {
        currentWeeks = 1
        longestWeeks = maxOf(longestWeeks, 1)
        lastAchievedWeek = weekStart
        updatedAt = LocalDateTime.now()
    }

    private fun weekStartOf(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

    companion object {
        fun create(userId: UUID): WeeklyRetroStreak = WeeklyRetroStreak(userId = userId)
    }
}
