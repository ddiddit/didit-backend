package com.didit.adapter.persistence.achievement

import com.didit.application.achievement.provided.AchievementDeletionPort
import com.didit.application.achievement.required.DailyAccessStreakRepository
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.application.achievement.required.UserLevelRepository
import com.didit.application.achievement.required.UserMissionRepository
import com.didit.application.achievement.required.WeeklyRetroStreakRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AchievementDeletionAdapter(
    private val userBadgeRepository: UserBadgeRepository,
    private val userMissionRepository: UserMissionRepository,
    private val userLevelRepository: UserLevelRepository,
    private val weeklyRetroStreakRepository: WeeklyRetroStreakRepository,
    private val dailyAccessStreakRepository: DailyAccessStreakRepository,
) : AchievementDeletionPort {
    override fun deleteByUserId(userId: UUID) {
        userBadgeRepository.deleteAllByUserId(userId)
        userMissionRepository.deleteAllByUserId(userId)
        userLevelRepository.deleteAllByUserId(userId)
        weeklyRetroStreakRepository.deleteAllByUserId(userId)
        dailyAccessStreakRepository.deleteAllByUserId(userId)
    }
}
