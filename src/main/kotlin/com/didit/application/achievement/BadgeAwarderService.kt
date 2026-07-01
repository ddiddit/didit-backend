package com.didit.application.achievement

import com.didit.application.achievement.provided.BadgeAwarder
import com.didit.application.achievement.provided.DailyAccessTracker
import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.DailyAccessStreakRepository
import com.didit.application.achievement.required.OrganizationAchievementReader
import com.didit.application.achievement.required.RetrospectAchievementReader
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.application.achievement.required.WeeklyRetroStreakRepository
import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.BadgeConditionType
import com.didit.domain.achievement.DailyAccessStreak
import com.didit.domain.achievement.UserBadge
import com.didit.domain.achievement.WeeklyRetroStreak
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

@Transactional(readOnly = true)
@Service
class BadgeAwarderService(
    private val badgeRepository: BadgeRepository,
    private val userBadgeRepository: UserBadgeRepository,
    private val weeklyRetroStreakRepository: WeeklyRetroStreakRepository,
    private val dailyAccessStreakRepository: DailyAccessStreakRepository,
    private val dailyAccessTracker: DailyAccessTracker,
    private val retrospectAchievementReader: RetrospectAchievementReader,
    private val organizationAchievementReader: OrganizationAchievementReader,
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
        val weeklyStreak = updateWeeklyRetroStreak(userId, retroDate)
        val dailyStreak = dailyAccessStreakRepository.findByUserId(userId) ?: DailyAccessStreak.create(userId)
        val context = buildContext(userId, retroDate, weeklyStreak, dailyStreak)
        val acquiredBadgeIds = userBadgeRepository.findAllByUserId(userId).map { it.badgeId }.toSet()

        val satisfied =
            badgeRepository
                .findAll()
                .filter { it.active }
                .filter { it.id !in acquiredBadgeIds }
                .filter { badgeConditionChecker.isSatisfied(it.condition, context) }

        return awardSatisfied(userId, satisfied)
    }

    @Transactional
    override fun awardAccessBadges(
        userId: UUID,
        accessDateKst: LocalDate,
    ): List<Badge> {
        val dailyStreak = dailyAccessTracker.recordAccess(userId, accessDateKst)
        val acquiredBadgeIds = userBadgeRepository.findAllByUserId(userId).map { it.badgeId }.toSet()

        val satisfied =
            badgeRepository
                .findAll()
                .filter { it.active }
                .filter { it.condition.conditionType == BadgeConditionType.DAILY_ACCESS_STREAK }
                .filter { it.id !in acquiredBadgeIds }
                .filter { dailyStreak.isStreak(it.condition.threshold) }

        return awardSatisfied(userId, satisfied)
    }

    private fun awardSatisfied(
        userId: UUID,
        satisfied: List<Badge>,
    ): List<Badge> {
        satisfied.forEach { badge ->
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
                        "conditionType" to badge.condition.conditionType.name,
                        "threshold" to badge.condition.threshold,
                    ),
            )
        }

        if (satisfied.isNotEmpty()) {
            logger.info("배지 획득 - userId: $userId, badges: ${satisfied.map { it.name }}")
        }

        return satisfied
    }

    private fun updateWeeklyRetroStreak(
        userId: UUID,
        retroDate: LocalDate,
    ): WeeklyRetroStreak {
        val streak = weeklyRetroStreakRepository.findByUserId(userId) ?: WeeklyRetroStreak.create(userId)
        streak.recordRetro(retroDate)
        return weeklyRetroStreakRepository.save(streak)
    }

    private fun buildContext(
        userId: UUID,
        retroDate: LocalDate,
        weeklyStreak: WeeklyRetroStreak,
        dailyStreak: DailyAccessStreak,
    ): BadgeCheckContext =
        BadgeCheckContext(
            userId = userId,
            retroDate = retroDate,
            totalRetroCount = retrospectAchievementReader.countCompletedRetros(userId),
            currentWeekRetroCount = retrospectAchievementReader.countRetrosInWeek(userId, retroDate.with(DayOfWeek.MONDAY)),
            weeklyRetroStreak = weeklyStreak,
            weeklyStreakWithMin3 = retrospectAchievementReader.countConsecutiveWeeksWithMinRetros(userId, 3),
            dailyAccessStreak = dailyStreak,
            projectCount = organizationAchievementReader.countProjects(userId),
            projectAssignedRetroCount = organizationAchievementReader.countProjectAssignedRetros(userId),
            maxRetroInOneProject = organizationAchievementReader.maxRetroCountInOneProject(userId),
        )
}
