package com.didit.application.achievement.required

import com.didit.domain.achievement.UserLevel
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import java.util.UUID

interface UserLevelRepository : Repository<UserLevel, UUID> {
    fun findByUserId(userId: UUID): UserLevel?

    fun save(userLevel: UserLevel): UserLevel

    fun existsByUserId(userId: UUID): Boolean

    @Query(
        "SELECT ul.currentLevel AS level, COUNT(ul) AS count FROM UserLevel ul " +
            "GROUP BY ul.currentLevel ORDER BY ul.currentLevel",
    )
    fun countGroupByLevel(): List<LevelCount>
}

interface LevelCount {
    fun getLevel(): Int

    fun getCount(): Long
}
