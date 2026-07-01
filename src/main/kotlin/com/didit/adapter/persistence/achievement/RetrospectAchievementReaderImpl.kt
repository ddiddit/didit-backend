package com.didit.adapter.persistence.achievement

import com.didit.application.achievement.required.RetrospectAchievementReader
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.RetroStatus
import com.didit.domain.shared.ServiceTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Component
class RetrospectAchievementReaderImpl(
    private val retrospectiveRepository: RetrospectiveRepository,
    @Value("\${achievement.feature-launched-at:2026-06-30T00:00:00}") private val featureLaunchedAt: LocalDateTime,
) : RetrospectAchievementReader {
    companion object {
        private const val MAX_CONSECUTIVE_WEEKS_SCAN = 60
    }

    override fun countCompletedRetros(userId: UUID): Int = completedAtsSinceLaunch(userId).size

    override fun countRetrosInWeek(
        userId: UUID,
        weekStartKst: LocalDate,
    ): Int = weekCountsByMondayKst(userId)[weekStartKst] ?: 0

    override fun countConsecutiveWeeksWithMinRetros(
        userId: UUID,
        weeklyMinCount: Int,
    ): Int {
        val weekCounts = weekCountsByMondayKst(userId)
        var weekStart = ServiceTime.today().with(DayOfWeek.MONDAY)
        var consecutive = 0
        repeat(MAX_CONSECUTIVE_WEEKS_SCAN) {
            val count = weekCounts[weekStart] ?: 0
            if (count < weeklyMinCount) return consecutive
            consecutive += 1
            weekStart = weekStart.minusWeeks(1)
        }
        return consecutive
    }

    private fun weekCountsByMondayKst(userId: UUID): Map<LocalDate, Int> =
        completedAtsSinceLaunch(userId)
            .map { ServiceTime.toServiceDate(it) }
            .groupingBy { it.with(DayOfWeek.MONDAY) }
            .eachCount()

    private fun completedAtsSinceLaunch(userId: UUID): List<LocalDateTime> =
        retrospectiveRepository
            .findCompletedAtByUserIdAndStatusAndDeletedAtIsNull(userId, RetroStatus.COMPLETED)
            .filter { !it.isBefore(featureLaunchedAt) }
}
