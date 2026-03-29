package com.didit.adapter.webapi.home

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.home.dto.HomeResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.provided.RetrospectiveFinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RequestMapping("/api/v1/home")
@RestController
class HomeApi(
    private val userFinder: UserFinder,
    private val retrospectiveFinder: RetrospectiveFinder,
) {
    @RequireAuth
    @GetMapping
    fun getHome(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<HomeResponse> {
        val user = userFinder.findByIdOrThrow(userId)
        val recentRetros = retrospectiveFinder.findRecentByUserId(userId, limit = 5)
        val todayCount = retrospectiveFinder.countByUserIdAndDate(userId, LocalDate.now())
        val latestFeedback =
            retrospectiveFinder
                .findLatestCompletedByUserId(userId)
                ?.summary
                ?.feedback

        return SuccessResponse.of(
            HomeResponse(
                nickname = user.nickname ?: "",
                todayRetrospectiveCount = todayCount,
                recentRetrospectives =
                    recentRetros.map {
                        HomeResponse.RecentRetrospectiveResponse.from(it)
                    },
                latestFeedback = latestFeedback,
            ),
        )
    }
}
