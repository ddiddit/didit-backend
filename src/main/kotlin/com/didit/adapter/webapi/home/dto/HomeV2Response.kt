package com.didit.adapter.webapi.home.dto

import com.didit.adapter.webapi.retrospect.dto.RetrospectiveListItemV2Response

data class HomeV2Response(
    val nickname: String,
    val todayRetrospectiveCount: Int,
    val recentRetrospectives: List<RetrospectiveListItemV2Response>,
)
