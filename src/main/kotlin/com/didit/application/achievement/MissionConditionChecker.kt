package com.didit.application.achievement

import com.didit.application.achievement.required.UserMissionRepository
import com.didit.domain.achievement.Mission
import com.didit.domain.achievement.MissionStatus
import com.didit.domain.achievement.MissionType
import com.didit.domain.achievement.UserMission
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class MissionConditionChecker(
    private val userMissionRepository: UserMissionRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MissionConditionChecker::class.java)
    }

    fun checkAndUpdate(
        userMission: UserMission,
        userId: UUID,
        retroDate: LocalDate,
        mission: Mission,
    ): Boolean =
        when (mission.missionType) {
            MissionType.FIRST_RETRO -> checkFirstRetro(userMission, mission)
            MissionType.TIME_LIMITED -> checkTimeLimited(userMission, userId, mission)
            MissionType.CONSECUTIVE_WEEK -> checkConsecutiveWeek(userMission, retroDate, mission)
            MissionType.CUMULATIVE_RETRO -> checkCumulativeRetro(userMission, userId, mission)
        }

    private fun checkFirstRetro(
        userMission: UserMission,
        mission: Mission,
    ): Boolean {
        userMission.incrementProgress()
        return true
    }

    private fun checkTimeLimited(
        userMission: UserMission,
        userId: UUID,
        mission: Mission,
    ): Boolean {
        checkAndHandleLv2Expiry(userMission, mission)

        if (userMission.status != MissionStatus.IN_PROGRESS) {
            return false
        }

        val retroCount =
            userMissionRepository.countRetrosAfter(
                userId = userId,
                startedAt = userMission.startedAt,
            )

        userMission.progress = retroCount

        return retroCount >= mission.targetCount
    }

    private fun checkAndHandleLv2Expiry(
        userMission: UserMission,
        mission: Mission,
    ) {
        val durationDays = mission.durationDays ?: 7
        val expiryDate = userMission.startedAt.toLocalDate().plusDays(durationDays.toLong())

        if (LocalDate.now() > expiryDate && userMission.progress < mission.targetCount) {
            userMission.setFailureWaitingConfirm()
            logger.info(
                "Lv.2 미션 만료로 실패 - userId: ${userMission.userId}, " +
                    "progress: ${userMission.progress}, expired: $expiryDate",
            )
        }
    }

    private fun checkConsecutiveWeek(
        userMission: UserMission,
        retroDate: LocalDate,
        mission: Mission,
    ): Boolean {
        val thisWeekStart = getWeekStartDate(retroDate)
        val lastRetroDate = userMission.lastRetroDate

        if (lastRetroDate == null) {
            userMission.progress = 1
        } else {
            val lastWeekStart = getWeekStartDate(lastRetroDate)
            when {
                thisWeekStart == lastWeekStart -> Unit
                thisWeekStart == lastWeekStart.plusWeeks(1) -> userMission.incrementProgress()
                else -> userMission.progress = 1
            }
        }

        if (lastRetroDate == null || retroDate.isAfter(lastRetroDate)) {
            userMission.lastRetroDate = retroDate
        }

        return userMission.progress >= mission.targetCount
    }

    private fun getWeekStartDate(date: LocalDate): LocalDate {
        val dayOfWeek = date.dayOfWeek.value
        return date.minusDays((dayOfWeek - 1).toLong())
    }

    private fun checkCumulativeRetro(
        userMission: UserMission,
        userId: UUID,
        mission: Mission,
    ): Boolean {
        val retroCount =
            userMissionRepository.countRetrosAfter(
                userId = userId,
                startedAt = userMission.startedAt,
            )

        userMission.progress = retroCount

        return retroCount >= mission.targetCount
    }
}
