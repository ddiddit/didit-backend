package com.didit.domain.achievement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "daily_access_streaks")
@Entity
class DailyAccessStreak(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(nullable = false)
    var currentStreak: Int = 0,
    @Column(nullable = false)
    var longestStreak: Int = 0,
    @Column
    var lastAccessDate: LocalDate? = null,
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun recordAccess(accessDateKst: LocalDate) {
        if (isSameDay(accessDateKst)) return
        if (isConsecutive(accessDateKst)) return increaseStreak(accessDateKst)
        resetStreak(accessDateKst)
    }

    fun isStreak(days: Int): Boolean = currentStreak >= days

    private fun isSameDay(accessDate: LocalDate): Boolean = lastAccessDate == accessDate

    private fun isConsecutive(accessDate: LocalDate): Boolean = lastAccessDate?.plusDays(1) == accessDate

    private fun increaseStreak(accessDate: LocalDate) {
        currentStreak += 1
        longestStreak = maxOf(longestStreak, currentStreak)
        lastAccessDate = accessDate
        updatedAt = LocalDateTime.now()
    }

    private fun resetStreak(accessDate: LocalDate) {
        currentStreak = 1
        longestStreak = maxOf(longestStreak, 1)
        lastAccessDate = accessDate
        updatedAt = LocalDateTime.now()
    }

    companion object {
        fun create(userId: UUID): DailyAccessStreak = DailyAccessStreak(userId = userId)
    }
}
