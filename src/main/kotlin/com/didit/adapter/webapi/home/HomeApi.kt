package com.didit.adapter.webapi.home

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.home.dto.HomeResponse
import com.didit.adapter.webapi.home.dto.HomeV2Response
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.adapter.webapi.retrospect.dto.RetrospectiveListItemV2Response
import com.didit.application.auth.provided.UserFinder
import com.didit.application.notification.provided.NotificationHistoryFinder
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.domain.shared.ServiceTime
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class HomeApi(
    private val userFinder: UserFinder,
    private val retrospectiveFinder: RetrospectiveFinder,
    private val notificationHistoryFinder: NotificationHistoryFinder,
) {
    @Deprecated("Use v2 endpoint", replaceWith = ReplaceWith("getHomeV2"))
    @RequireAuth
    @GetMapping("/api/v1/home")
    fun getHome(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<HomeResponse> {
        val user = userFinder.findByIdOrThrow(userId)
        val recentRetros = retrospectiveFinder.findRecentByUserId(userId, limit = 5)
        val todayCount = retrospectiveFinder.countByUserIdAndDate(userId, ServiceTime.today())

        return SuccessResponse.of(
            HomeResponse(
                nickname = user.nickname ?: "",
                todayRetrospectiveCount = todayCount,
                recentRetrospectives =
                    recentRetros.map {
                        HomeResponse.RecentRetrospectiveResponse.from(it)
                    },
            ),
        )
    }

    @RequireAuth
    @GetMapping("/api/v2/home")
    fun getHomeV2(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<HomeV2Response> {
        val user = userFinder.findByIdOrThrow(userId)
        val recentRetros = retrospectiveFinder.findRecentWithProjectAndTagsByUserId(userId, limit = 5)
        val todayCount = retrospectiveFinder.countByUserIdAndDate(userId, ServiceTime.today())

        return SuccessResponse.of(
            HomeV2Response(
                nickname = user.nickname ?: "",
                todayRetrospectiveCount = todayCount,
                hasUnreadNotification = notificationHistoryFinder.hasUnread(userId),
                recentRetrospectives =
                    recentRetros.map {
                        RetrospectiveListItemV2Response.from(it)
                    },
            ),
        )
    }
}
