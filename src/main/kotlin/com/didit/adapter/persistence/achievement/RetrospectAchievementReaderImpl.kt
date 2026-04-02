package com.didit.adapter.persistence.achievement

import com.didit.application.achievement.required.RetrospectAchievementReader
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.RetroStatus
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.util.UUID

@Component
class RetrospectAchievementReaderImpl(
    private val retrospectiveRepository: RetrospectiveRepository,
) : RetrospectAchievementReader {
    override fun countCompletedRetros(userId: UUID): Int =
        retrospectiveRepository.countByUserIdAndStatusAndDeletedAtIsNull(userId, RetroStatus.COMPLETED)

    override fun countDeepQuestionAnswers(userId: UUID): Int =
        retrospectiveRepository
            .findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
            .sumOf { it.countDeepQuestionAnswers() }

    override fun countWeeklyGoalAchievedWeeks(userId: UUID): Int =
        retrospectiveRepository
            .findCompletedAtByUserIdAndStatusAndDeletedAtIsNull(userId, RetroStatus.COMPLETED)
            .groupBy { it.toLocalDate().with(DayOfWeek.MONDAY) }
            .count { (_, dates) -> dates.size >= 3 }
}
