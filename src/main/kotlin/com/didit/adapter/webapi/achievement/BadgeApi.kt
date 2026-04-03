package com.didit.adapter.webapi.achievement

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.achievement.dto.BadgeResponse
import com.didit.application.achievement.provided.BadgeFinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/badges")
@RestController
class BadgeApi(
    private val badgeFinder: BadgeFinder,
) {
    @RequireAuth
    @GetMapping
    fun findAll(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<List<BadgeResponse>> = SuccessResponse.of(badgeFinder.findAll(userId))

    @RequireAuth
    @GetMapping("/popup")
    fun findUnnotified(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<List<BadgeResponse>> = SuccessResponse.of(badgeFinder.findUnnotified(userId))
}
