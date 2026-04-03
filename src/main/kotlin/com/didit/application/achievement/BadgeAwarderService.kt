package com.didit.application.achievement

import com.didit.application.achievement.provided.BadgeAwarder
import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.RetrospectAchievementReader
import com.didit.application.achievement.required.StreakRepository
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.Streak
import com.didit.domain.achievement.UserBadge
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Transactional(readOnly = true)
@Service
class BadgeAwarderService(
    private val badgeRepository: BadgeRepository,
    private val userBadgeRepository: UserBadgeRepository,
    private val streakRepository: StreakRepository,
    private val retrospectAchievementReader: RetrospectAchievementReader,
    private val badgeConditionChecker: BadgeConditionChecker,
    private val auditLogger: AuditLogger,
) : BadgeAwarder {
    companion object {
        private val logger = LoggerFactory.getLogger(BadgeAwarderService::class.java)
    }

    @Transactional
    override fun awardBadges(
        userId: UUID,
        retroDate: LocalDate,
    ): List<Badge> {
        val streak = updateStreak(userId, retroDate)
        val context = buildContext(userId, retroDate, streak)

        val allBadges = badgeRepository.findAll()
        val acquiredBadgeIds = userBadgeRepository.findAllByUserId(userId).map { it.badgeId }.toSet()

        val newBadges =
            allBadges
                .filter { badge -> badge.id !in acquiredBadgeIds }
                .filter { badge -> badgeConditionChecker.isSatisfied(badge.conditionType, context) }
                .onEach { badge ->
                    userBadgeRepository.save(UserBadge.create(userId, badge.id))

                    auditLogger.log(
                        actorId = userId,
                        actorType = ActorType.SYSTEM,
                        action = AuditAction.BADGE_ACQUIRED,
                        targetId = badge.id,
                        targetType = "BADGE",
                        payload =
                            mapOf(
                                "badgeName" to badge.name,
                                "conditionType" to badge.conditionType.name,
                            ),
                    )
                }

        if (newBadges.isNotEmpty()) {
            logger.info("배지 획득 - userId: $userId, badges: ${newBadges.map { it.name }}")
        }

        return newBadges
    }

    private fun updateStreak(
        userId: UUID,
        retroDate: LocalDate,
    ): Streak {
        val streak = streakRepository.findByUserId(userId) ?: Streak.create(userId)
        streak.update(retroDate)
        return streakRepository.save(streak)
    }

    private fun buildContext(
        userId: UUID,
        retroDate: LocalDate,
        streak: Streak,
    ): BadgeCheckContext =
        BadgeCheckContext(
            userId = userId,
            totalRetroCount = retrospectAchievementReader.countCompletedRetros(userId),
            streak = streak,
            deepQuestionCount = retrospectAchievementReader.countDeepQuestionAnswers(userId),
            retroDate = retroDate,
            weeklyGoalAchievedWeeks = retrospectAchievementReader.countWeeklyGoalAchievedWeeks(userId),
        )
}
