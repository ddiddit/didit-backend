package com.didit.application.admin

import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.application.admin.provided.AdminBadgeCategoryItem
import com.didit.application.admin.provided.AdminBadgeConditionTypeItem
import com.didit.application.admin.provided.AdminBadgeFinder
import com.didit.application.admin.provided.AdminBadgeHolder
import com.didit.application.admin.provided.AdminBadgeMetaResult
import com.didit.application.admin.provided.AdminBadgeParamSpec
import com.didit.application.admin.provided.AdminBadgeResult
import com.didit.application.auth.required.UserRepository
import com.didit.application.common.exception.BusinessException
import com.didit.application.common.exception.ErrorCode
import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.BadgeCategory
import com.didit.domain.achievement.BadgeConditionType
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
    override fun findAll(): List<AdminBadgeResult> = badgeRepository.findAll().map { it.toResult() }

    override fun findById(badgeId: UUID): AdminBadgeResult {
        val badge =
            badgeRepository.findById(badgeId)
                ?: throw BusinessException(ErrorCode.NOT_FOUND, "배지를 찾을 수 없습니다: $badgeId")
        return badge.toResult()
    }

    override fun findHolders(badgeId: UUID): List<AdminBadgeHolder> {
        val userBadges = userBadgeRepository.findAllByBadgeId(badgeId)
        if (userBadges.isEmpty()) return emptyList()

        val userMap =
            userBadges
                .mapNotNull { userRepository.findById(it.userId) }
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

    override fun findMeta(): AdminBadgeMetaResult =
        AdminBadgeMetaResult(
            conditionTypes =
                BadgeConditionType.entries.map { type ->
                    AdminBadgeConditionTypeItem(
                        conditionType = type.name,
                        label = conditionTypeLabel(type),
                        description = conditionTypeDescription(type),
                        params = conditionTypeParams(type),
                    )
                },
            categories =
                BadgeCategory.entries.map { category ->
                    AdminBadgeCategoryItem(
                        category = category.name,
                        label = categoryLabel(category),
                    )
                },
        )

    private fun Badge.toResult() =
        AdminBadgeResult(
            id = id,
            name = name,
            description = description,
            category = category.name,
            conditionType = condition.conditionType.name,
            threshold = condition.threshold,
            params = condition.params,
            iconUrl = iconUrl,
            congratsTitle = congratsTitle,
            congratsMessage = congratsMessage,
            active = active,
            acquiredCount = userBadgeRepository.countByBadgeId(id),
            createdAt = createdAt,
        )

    private fun conditionTypeLabel(type: BadgeConditionType): String =
        when (type) {
            BadgeConditionType.CUMULATIVE_RETRO -> "누적 회고 수"
            BadgeConditionType.WEEKLY_RETRO_COUNT -> "주간 회고 수"
            BadgeConditionType.WEEKLY_STREAK -> "연속 주차"
            BadgeConditionType.DAILY_ACCESS_STREAK -> "연속 접속일"
            BadgeConditionType.PROJECT_COUNT -> "프로젝트 생성 수"
            BadgeConditionType.PROJECT_TAGGED_RETRO -> "프로젝트 지정 회고 수"
            BadgeConditionType.PROJECT_RETRO_IN_ONE -> "단일 프로젝트 회고 수"
        }

    private fun conditionTypeDescription(type: BadgeConditionType): String =
        when (type) {
            BadgeConditionType.CUMULATIVE_RETRO -> "누적 회고 저장 수가 threshold 이상이면 부여"
            BadgeConditionType.WEEKLY_RETRO_COUNT -> "현재 주(월~일) 회고 수가 threshold 이상이면 부여"
            BadgeConditionType.WEEKLY_STREAK -> "매주 weeklyMinCount회 이상을 threshold주 연속 달성하면 부여"
            BadgeConditionType.DAILY_ACCESS_STREAK -> "연속 접속일이 threshold 이상이면 부여"
            BadgeConditionType.PROJECT_COUNT -> "생성한 프로젝트 수가 threshold 이상이면 부여"
            BadgeConditionType.PROJECT_TAGGED_RETRO -> "프로젝트가 지정된 회고 수가 threshold 이상이면 부여"
            BadgeConditionType.PROJECT_RETRO_IN_ONE -> "한 프로젝트에 속한 회고 수가 threshold 이상이면 부여"
        }

    private fun conditionTypeParams(type: BadgeConditionType): List<AdminBadgeParamSpec> =
        when (type) {
            BadgeConditionType.WEEKLY_STREAK ->
                listOf(
                    AdminBadgeParamSpec(
                        key = "weeklyMinCount",
                        label = "주당 최소 회고 수",
                        type = "number",
                        defaultValue = 1,
                        required = false,
                    ),
                )
            else -> emptyList()
        }

    private fun categoryLabel(category: BadgeCategory): String =
        when (category) {
            BadgeCategory.CONSISTENCY -> "꾸준함"
            BadgeCategory.PROJECT -> "프로젝트"
            BadgeCategory.PATTERN -> "패턴"
            BadgeCategory.ACCESS -> "접속"
        }
}
