package com.didit.application.admin

import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.application.achievement.required.UserLevelRepository
import com.didit.application.achievement.required.UserMissionRepository
import com.didit.application.admin.provided.AdminAchievementStatsFinder
import com.didit.application.admin.provided.AdminBadgeStat
import com.didit.application.admin.provided.AdminLevelStat
import com.didit.application.admin.provided.AdminMissionStat
import com.didit.application.auth.required.UserRepository
import com.didit.domain.achievement.MissionStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class AdminAchievementStatsService(
    private val userLevelRepository: UserLevelRepository,
    private val userMissionRepository: UserMissionRepository,
    private val badgeRepository: BadgeRepository,
    private val userBadgeRepository: UserBadgeRepository,
    private val userRepository: UserRepository,
) : AdminAchievementStatsFinder {
    companion object {
        private const val MIN_LEVEL = 1
        private const val MAX_LEVEL = 10
    }

    override fun getLevelStats(): List<AdminLevelStat> {
        val countByLevel = userLevelRepository.countGroupByLevel().associate { it.getLevel() to it.getCount() }
        val usersWithLevelRow = countByLevel.values.sum()
        val usersWithoutLevelRow = (userRepository.countByDeletedAtIsNull() - usersWithLevelRow).coerceAtLeast(0)
        return (MIN_LEVEL..MAX_LEVEL).map { level ->
            val base = countByLevel[level] ?: 0L
            val userCount = if (level == MIN_LEVEL) base + usersWithoutLevelRow else base
            AdminLevelStat(level = level, userCount = userCount)
        }
    }

    override fun getMissionStats(): List<AdminMissionStat> {
        val rows = userMissionRepository.countGroupByLevelAndStatus()
        val byLevel = rows.groupBy { it.getLevel() }

        return byLevel.entries
            .sortedBy { it.key }
            .map { (level, statusRows) ->
                val countByStatus = statusRows.associate { it.getStatus() to it.getCount() }
                val inProgress = countByStatus[MissionStatus.IN_PROGRESS] ?: 0L
                val completed = countByStatus[MissionStatus.COMPLETED] ?: 0L
                val failed =
                    (countByStatus[MissionStatus.FAILED] ?: 0L) + (countByStatus[MissionStatus.WAIT_CONFIRM] ?: 0L)
                val total = inProgress + completed + failed
                AdminMissionStat(
                    level = level,
                    inProgress = inProgress,
                    completed = completed,
                    failed = failed,
                    total = total,
                    completionRate = if (total == 0L) 0.0 else completed.toDouble() / total * 100,
                )
            }
    }

    override fun getBadgeStats(): List<AdminBadgeStat> =
        badgeRepository.findAll().map { badge ->
            AdminBadgeStat(
                badgeId = badge.id,
                name = badge.name,
                category = badge.category.name,
                conditionType = badge.condition.conditionType.name,
                active = badge.active,
                acquiredCount = userBadgeRepository.countByBadgeId(badge.id),
            )
        }
}
