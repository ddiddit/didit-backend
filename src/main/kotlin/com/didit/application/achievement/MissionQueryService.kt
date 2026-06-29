package com.didit.application.achievement

import com.didit.application.achievement.dto.CurrentMissionResponse
import com.didit.application.achievement.dto.MissionInfo
import com.didit.application.achievement.dto.PopupStatus
import com.didit.application.achievement.dto.WeeklyStatus
import com.didit.application.achievement.exception.CurrentMissionNotFoundException
import com.didit.application.achievement.exception.MissionDefinitionNotFoundException
import com.didit.application.achievement.exception.UserLevelNotFoundException
import com.didit.application.achievement.provided.MissionFinder
import com.didit.application.achievement.required.MissionRepository
import com.didit.application.achievement.required.UserLevelRepository
import com.didit.application.achievement.required.UserMissionRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.achievement.Mission
import com.didit.domain.achievement.MissionType
import com.didit.domain.achievement.UserMission
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@Service
@Transactional(readOnly = true)
class MissionQueryService(
    private val userLevelRepository: UserLevelRepository,
    private val userMissionRepository: UserMissionRepository,
    private val missionRepository: MissionRepository,
    private val retrospectiveRepository: RetrospectiveRepository,
) : MissionFinder {
    override fun getCurrentMission(userId: UUID): CurrentMissionResponse {
        val userLevel = userLevelRepository.findByUserId(userId) ?: throw UserLevelNotFoundException(userId)
        val userMission =
            userMissionRepository.findCurrentMissionByUserId(userId) ?: throw CurrentMissionNotFoundException(userId)
        val mission =
            missionRepository.findByLevel(userLevel.currentLevel) ?: throw MissionDefinitionNotFoundException(
                userLevel.currentLevel,
            )

        val missionInfo =
            MissionInfo(
                type = mission.missionType.name,
                title = mission.title,
                description = mission.description,
                progress = userMission.progress,
                target = mission.targetCount,
                remainingDays = calculateDaysRemaining(mission, userMission),
                cta = getMissionCta(mission),
            )

        val weeklyStatus = buildWeeklyStatus(mission, userMission, userId)
        val popupStatus = getPopupStatus(userMission)

        return CurrentMissionResponse(
            currentLevel = userLevel.currentLevel,
            mission = missionInfo,
            weeklyStatus = weeklyStatus,
            popup = popupStatus,
        )
    }

    private fun calculateDaysRemaining(
        mission: Mission,
        userMission: UserMission,
    ): Int? {
        if (mission.missionType != MissionType.TIME_LIMITED) {
            return null
        }

        val durationDays = mission.durationDays ?: return null
        val startDate = userMission.startedAt.toLocalDate()
        val endDate = startDate.plusDays(durationDays.toLong())
        val today = LocalDate.now()

        return (endDate.toEpochDay() - today.toEpochDay()).toInt()
    }

    private fun buildWeeklyStatus(
        mission: Mission,
        userMission: UserMission,
        userId: UUID,
    ): WeeklyStatus? {
        if (mission.missionType != MissionType.CONSECUTIVE_WEEK) {
            return null
        }

        val weekStartDate = getWeekStartDate(LocalDate.now())
        val weekEndDate = weekStartDate.plusDays(6)
        val from = LocalDateTime.of(weekStartDate, LocalTime.MIDNIGHT)
        val to = LocalDateTime.of(weekEndDate, LocalTime.of(23, 59, 59))

        val completedRetros = retrospectiveRepository.findCompletedByUserIdAndPeriod(userId, from, to)
        val completedDates = completedRetros.map { it.completedAt?.toLocalDate() }.toSet()

        val today = LocalDate.now()
        val days = mutableListOf<Boolean>()

        for (i in 0..6) {
            val dayDate = weekStartDate.plusDays(i.toLong())
            val isCompleted =
                if (dayDate > today) {
                    false
                } else {
                    completedDates.contains(dayDate)
                }
            days.add(isCompleted)
        }

        return WeeklyStatus(
            show = true,
            weekStart = weekStartDate,
            days = days,
        )
    }

    private fun getPopupStatus(userMission: UserMission): PopupStatus =
        when {
            !userMission.levelUpPopupShown -> PopupStatus(exists = true, type = "LEVEL_UP")
            !userMission.failurePopupShown -> PopupStatus(exists = true, type = "FAILURE")
            else -> PopupStatus(exists = false, type = null)
        }

    private fun getMissionCta(mission: Mission): String = "회고 남기기"

    private fun getWeekStartDate(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
