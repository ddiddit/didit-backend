package com.didit.adapter.retrospect.`in`.web.response

import com.didit.domain.retrospect.enums.RetroStatus
import java.util.UUID

data class RetrospectiveResponse(
    val retrospectiveId: UUID,
    val status: RetroStatus,
    val summary: RetrospectiveSummaryResponse?
)