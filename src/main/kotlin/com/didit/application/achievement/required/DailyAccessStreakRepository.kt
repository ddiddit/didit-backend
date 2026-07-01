package com.didit.application.achievement.required

import com.didit.domain.achievement.DailyAccessStreak
import org.springframework.data.repository.Repository
import java.util.UUID

interface DailyAccessStreakRepository : Repository<DailyAccessStreak, UUID> {
    fun findByUserId(userId: UUID): DailyAccessStreak?

    fun save(streak: DailyAccessStreak): DailyAccessStreak

    fun deleteAllByUserId(userId: UUID)
}
