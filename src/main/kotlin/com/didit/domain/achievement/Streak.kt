package com.didit.domain.achievement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "streaks")
@Entity
class Streak(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(nullable = false)
    var currentStreak: Int = 0,
    @Column(nullable = false)
    var longestStreak: Int = 0,
    @Column
    var lastRetroDate: LocalDate? = null,
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun update(retroDate: LocalDate) {
        if (isSameDay(retroDate)) return
        if (isConsecutive(retroDate)) return increaseStreak(retroDate)
        resetStreak(retroDate)
    }

    fun isStreak(days: Int): Boolean = currentStreak >= days

    private fun isSameDay(retroDate: LocalDate): Boolean = lastRetroDate == retroDate

    private fun isConsecutive(retroDate: LocalDate): Boolean = lastRetroDate?.plusDays(1) == retroDate

    private fun increaseStreak(retroDate: LocalDate) {
        currentStreak += 1
        longestStreak = maxOf(longestStreak, currentStreak)
        lastRetroDate = retroDate
        updatedAt = LocalDateTime.now()
    }

    private fun resetStreak(retroDate: LocalDate) {
        currentStreak = 1
        longestStreak = maxOf(longestStreak, 1)
        lastRetroDate = retroDate
        updatedAt = LocalDateTime.now()
    }

    companion object {
        fun create(userId: UUID): Streak = Streak(userId = userId)
    }
}
