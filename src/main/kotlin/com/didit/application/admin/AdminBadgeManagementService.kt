package com.didit.application.admin

import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.application.admin.provided.AdminBadgeCreateCommand
import com.didit.application.admin.provided.AdminBadgeRegister
import com.didit.application.admin.provided.AdminBadgeResult
import com.didit.application.admin.provided.AdminBadgeUpdateCommand
import com.didit.application.common.exception.BusinessException
import com.didit.application.common.exception.ErrorCode
import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.BadgeCategory
import com.didit.domain.achievement.BadgeCondition
import com.didit.domain.achievement.BadgeConditionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
@Service
class AdminBadgeManagementService(
    private val badgeRepository: BadgeRepository,
    private val userBadgeRepository: UserBadgeRepository,
) : AdminBadgeRegister {
    override fun create(command: AdminBadgeCreateCommand): AdminBadgeResult {
        val badge =
            Badge.create(
                name = command.name,
                description = command.description,
                category = parseCategory(command.category),
                condition =
                    buildCondition(
                        conditionType = command.conditionType,
                        threshold = command.threshold,
                        params = command.params,
                    ),
                iconUrl = command.iconUrl,
                congratsTitle = command.congratsTitle,
                congratsMessage = command.congratsMessage,
            )
        return badgeRepository.save(badge).toResult()
    }

    override fun update(
        badgeId: UUID,
        command: AdminBadgeUpdateCommand,
    ): AdminBadgeResult {
        val badge = findBadge(badgeId)
        badge.update(
            name = command.name,
            description = command.description,
            category = parseCategory(command.category),
            condition =
                buildCondition(
                    conditionType = command.conditionType,
                    threshold = command.threshold,
                    params = command.params,
                ),
            iconUrl = command.iconUrl,
            congratsTitle = command.congratsTitle,
            congratsMessage = command.congratsMessage,
        )
        return badgeRepository.save(badge).toResult()
    }

    override fun changeActive(
        badgeId: UUID,
        active: Boolean,
    ): AdminBadgeResult {
        val badge = findBadge(badgeId)
        badge.changeActive(active)
        return badgeRepository.save(badge).toResult()
    }

    private fun findBadge(badgeId: UUID): Badge =
        badgeRepository.findById(badgeId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND, "배지를 찾을 수 없습니다: $badgeId")

    private fun buildCondition(
        conditionType: String,
        threshold: Int,
        params: Map<String, Any>?,
    ): BadgeCondition {
        if (threshold < 1) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "threshold는 1 이상이어야 합니다: $threshold")
        }
        return BadgeCondition(
            conditionType = parseConditionType(conditionType),
            threshold = threshold,
            params = params?.takeIf { it.isNotEmpty() },
        )
    }

    private fun parseCategory(value: String): BadgeCategory =
        BadgeCategory.entries.find { it.name == value }
            ?: throw BusinessException(ErrorCode.INVALID_REQUEST, "유효하지 않은 category 값: $value")

    private fun parseConditionType(value: String): BadgeConditionType =
        BadgeConditionType.entries.find { it.name == value }
            ?: throw BusinessException(ErrorCode.INVALID_REQUEST, "유효하지 않은 conditionType 값: $value")

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
}
