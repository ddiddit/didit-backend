package com.didit.adapter.webapi.achievement.dto

import com.didit.domain.achievement.Mission
import com.didit.domain.achievement.UserLevel
import com.didit.domain.achievement.UserMission
import java.util.UUID

data class CurrentMissionResponse(
    val currentLevel: Int,
    val missionId: UUID,
    val missionType: String,
    val title: String,
    val description: String,
    val subText: String?,
    val status: String,
    val progress: Int,
    val targetCount: Int,
    val daysRemaining: Int? = null,
    val weeklyRetroStatus: WeeklyRetroStatus? = null,
    val isLevelUpPopupShown: Boolean = false,
    val isFailurePopupShown: Boolean = false,
) {
    companion object {
        fun from(
            userLevel: UserLevel,
            userMission: UserMission,
            mission: Mission,
            daysRemaining: Int? = null,
            weeklyRetroStatus: WeeklyRetroStatus? = null,
        ): CurrentMissionResponse =
            CurrentMissionResponse(
                currentLevel = userLevel.currentLevel,
                missionId = userMission.id,
                missionType = mission.missionType.name,
                title = mission.title,
                description = mission.description,
                subText = mission.subText,
                status = userMission.status.name,
                progress = userMission.progress,
                targetCount = mission.targetCount,
                daysRemaining = daysRemaining,
                weeklyRetroStatus = weeklyRetroStatus,
                isLevelUpPopupShown = userMission.levelUpPopupShown,
                isFailurePopupShown = userMission.failurePopupShown,
            )
    }
}
