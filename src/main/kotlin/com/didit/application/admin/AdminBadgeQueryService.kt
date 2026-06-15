package com.didit.application.admin

import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.application.admin.provided.AdminBadgeFinder
import com.didit.application.admin.provided.AdminBadgeHolder
import com.didit.application.admin.provided.AdminBadgeResult
import com.didit.application.auth.required.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class AdminBadgeQueryService(
    private val badgeRepository: BadgeRepository,
    private val userBadgeRepository: UserBadgeRepository,
    private val userRepository: UserRepository,
) : AdminBadgeFinder {
    override fun findAll(): List<AdminBadgeResult> =
        badgeRepository.findAll().map { badge ->
            AdminBadgeResult(
                id = badge.id,
                name = badge.name,
                description = badge.description,
                conditionType = badge.conditionType.name,
                acquiredCount = userBadgeRepository.countByBadgeId(badge.id),
                createdAt = badge.createdAt,
            )
        }

    override fun findHolders(badgeId: UUID): List<AdminBadgeHolder> {
        val userBadges = userBadgeRepository.findAllByBadgeId(badgeId)
        val userIds = userBadges.map { it.userId }
        val userMap = userIds
            .mapNotNull { userRepository.findById(it) }
            .associateBy { it.id }

        return userBadges.map { ub ->
            val user = userMap[ub.userId]
            AdminBadgeHolder(
                userId = ub.userId,
                email = user?.email,
                nickname = user?.nickname,
                acquiredAt = ub.acquiredAt,
            )
        }
    }
}
