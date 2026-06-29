package com.didit.adapter.webapi.achievement

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.achievement.dto.CurrentMissionResponse
import com.didit.application.achievement.provided.MissionFinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/missions")
@RestController
class MissionApi(
    private val missionFinder: MissionFinder,
) {
    @RequireAuth
    @GetMapping("/current")
    fun getCurrentMission(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<CurrentMissionResponse> = SuccessResponse.of(missionFinder.getCurrentMission(userId))
}
