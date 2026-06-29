package com.didit.application.achievement

import com.didit.application.achievement.provided.DailyAccessTracker
import com.didit.application.achievement.required.DailyAccessStreakRepository
import com.didit.domain.achievement.DailyAccessStreak
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Transactional(readOnly = true)
@Service
class DailyAccessTrackingService(
    private val dailyAccessStreakRepository: DailyAccessStreakRepository,
) : DailyAccessTracker {
    companion object {
        private val logger = LoggerFactory.getLogger(DailyAccessTrackingService::class.java)
    }

    @Transactional
    override fun recordAccess(
        userId: UUID,
        accessDateKst: LocalDate,
    ): DailyAccessStreak {
        val streak = dailyAccessStreakRepository.findByUserId(userId) ?: DailyAccessStreak.create(userId)
        streak.recordAccess(accessDateKst)
        val saved = dailyAccessStreakRepository.save(streak)
        logger.info("일일 접속 스트릭 갱신 - userId: $userId, currentStreak: ${saved.currentStreak}")
        return saved
    }
}
