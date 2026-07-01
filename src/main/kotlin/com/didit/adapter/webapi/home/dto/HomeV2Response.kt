package com.didit.adapter.webapi.home.dto

import com.didit.adapter.webapi.retrospect.dto.RetrospectiveListItemV2Response
import com.didit.application.achievement.dto.CurrentMissionResponse

data class HomeV2Response(
    val nickname: String,
    val todayRetrospectiveCount: Int,
    val hasUnreadNotification: Boolean,
    val mission: CurrentMissionResponse,
    val recentRetrospectives: List<RetrospectiveListItemV2Response>,
)
