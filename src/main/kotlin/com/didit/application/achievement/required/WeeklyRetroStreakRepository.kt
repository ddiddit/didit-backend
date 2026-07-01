package com.didit.application.achievement.required

import com.didit.domain.achievement.WeeklyRetroStreak
import org.springframework.data.repository.Repository
import java.util.UUID

interface WeeklyRetroStreakRepository : Repository<WeeklyRetroStreak, UUID> {
    fun findByUserId(userId: UUID): WeeklyRetroStreak?

    fun save(streak: WeeklyRetroStreak): WeeklyRetroStreak
}
