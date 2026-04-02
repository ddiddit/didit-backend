package com.didit.application.achievement

import com.didit.application.achievement.dto.BadgeResponse
import com.didit.application.achievement.provided.BadgeFinder
import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.UserBadgeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class BadgeQueryService(
    private val badgeRepository: BadgeRepository,
    private val userBadgeRepository: UserBadgeRepository,
) : BadgeFinder {
    override fun findAll(userId: UUID): List<BadgeResponse> {
        val allBadges = badgeRepository.findAll()
        val userBadgeMap =
            userBadgeRepository
                .findAllByUserId(userId)
                .associateBy { it.badgeId }

        return allBadges.map { badge ->
            BadgeResponse.of(badge, userBadgeMap[badge.id])
        }
    }

    override fun findRecent(userId: UUID): List<BadgeResponse> {
        val recentUserBadges = userBadgeRepository.findTop3ByUserIdOrderByAcquiredAtDesc(userId)
        val badgeMap = badgeRepository.findAll().associateBy { it.id }

        return recentUserBadges.mapNotNull { userBadge ->
            badgeMap[userBadge.badgeId]?.let { badge ->
                BadgeResponse.of(badge, userBadge)
            }
        }
    }

    override fun findUnnotified(userId: UUID): List<BadgeResponse> {
        val unnotifiedUserBadges = userBadgeRepository.findAllByUserIdAndIsNotifiedFalse(userId)
        val badgeMap = badgeRepository.findAll().associateBy { it.id }

        return unnotifiedUserBadges
            .onEach { it.markAsNotified() }
            .onEach { userBadgeRepository.save(it) }
            .mapNotNull { userBadge ->
                badgeMap[userBadge.badgeId]?.let { badge ->
                    BadgeResponse.of(badge, userBadge)
                }
            }
    }
}
