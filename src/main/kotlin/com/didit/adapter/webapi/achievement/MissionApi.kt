package com.didit.adapter.webapi.achievement

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.achievement.dto.CurrentMissionResponse
import com.didit.application.achievement.provided.MissionFinder
import com.didit.application.achievement.provided.UserMissionRegister
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/missions")
@RestController
class MissionApi(
    private val missionFinder: MissionFinder,
    private val userMissionRegister: UserMissionRegister,
) {
    @RequireAuth
    @GetMapping("/current")
    fun getCurrentMission(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<CurrentMissionResponse> = SuccessResponse.of(missionFinder.getCurrentMission(userId))

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/level-up/confirm")
    fun confirmLevelUp(
        @CurrentUserId userId: UUID,
    ) {
        userMissionRegister.confirmLevelUp(userId)
    }
}
