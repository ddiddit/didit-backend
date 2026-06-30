package com.didit.application.achievement.required

import com.didit.domain.achievement.UserLevel
import org.springframework.data.repository.Repository
import java.util.UUID

interface UserLevelRepository : Repository<UserLevel, UUID> {
    fun findByUserId(userId: UUID): UserLevel?

    fun save(userLevel: UserLevel): UserLevel

    fun existsByUserId(userId: UUID): Boolean
}
