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
            MissionType.CONSECUTIVE_WEEK -> checkConsecutiveWeek(userMission, userId, retroDate, mission)
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

        if (userMission.status == MissionStatus.FAILED) {
            return false
        }

        val retroCount =
            userMissionRepository.countRetrosBetweenDates(
                userId = userId,
                startDate = userMission.startedAt.toLocalDate(),
                endDate = LocalDate.now(),
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
            userMission.fail()
            logger.info(
                "Lv.2 미션 만료로 실패 - userId: ${userMission.userId}, " +
                    "progress: ${userMission.progress}, expired: $expiryDate",
            )
        }
    }

    private fun checkConsecutiveWeek(
        userMission: UserMission,
        userId: UUID,
        retroDate: LocalDate,
        mission: Mission,
    ): Boolean {
        val weekStart = getWeekStartDate(retroDate)
        val weekEnd = weekStart.plusDays(6)

        val hasRetroThisWeek =
            userMissionRepository.countRetrosBetweenDates(
                userId = userId,
                startDate = weekStart,
                endDate = weekEnd,
            ) > 0

        if (hasRetroThisWeek) {
            userMission.incrementProgress()
            userMission.lastRetroDate = retroDate
        }

        return userMission.progress >= mission.targetCount
    }

    fun checkAndHandleWeeklyReset(
        userMission: UserMission,
        userId: UUID,
    ) {
        val lastWeekStart = getWeekStartDate(LocalDate.now().minusWeeks(1))
        val lastWeekEnd = lastWeekStart.plusDays(6)

        val hasRetroLastWeek =
            userMissionRepository.countRetrosBetweenDates(
                userId = userId,
                startDate = lastWeekStart,
                endDate = lastWeekEnd,
            ) > 0

        if (!hasRetroLastWeek && userMission.lastRetroDate != null) {
            userMission.resetProgress()
            logger.info(
                "연속 주차 미션 리셋 - userId: ${userMission.userId}, " +
                    "lastWeekStart: $lastWeekStart, lastWeekEnd: $lastWeekEnd",
            )
        }
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
            userMissionRepository.countRetrosBetweenDates(
                userId = userId,
                startDate = userMission.startedAt.toLocalDate(),
                endDate = LocalDate.now(),
            )

        userMission.progress = retroCount

        return retroCount >= mission.targetCount
    }
}
