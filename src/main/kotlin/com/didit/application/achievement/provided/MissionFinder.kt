package com.didit.application.achievement.provided

import com.didit.application.achievement.dto.CurrentMissionResponse
import java.util.UUID

interface MissionFinder {
    fun getCurrentMission(userId: UUID): CurrentMissionResponse
}
