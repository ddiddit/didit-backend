package com.didit.application.achievement.required

import com.didit.domain.achievement.UserBadge
import org.springframework.data.repository.Repository
import java.util.UUID

interface UserBadgeRepository : Repository<UserBadge, UUID> {
    fun findAllByUserId(userId: UUID): List<UserBadge>

    fun findTop3ByUserIdOrderByAcquiredAtDesc(userId: UUID): List<UserBadge>

    fun findAllByUserIdAndIsNotifiedFalse(userId: UUID): List<UserBadge>

    fun save(userBadge: UserBadge): UserBadge
}
