package com.didit.application.achievement.required

import com.didit.domain.achievement.Streak
import org.springframework.data.repository.Repository
import java.util.UUID

interface StreakRepository : Repository<Streak, UUID> {
    fun findByUserId(userId: UUID): Streak?

    fun save(streak: Streak): Streak
}
