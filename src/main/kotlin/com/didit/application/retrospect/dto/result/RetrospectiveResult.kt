package com.didit.application.retrospect.dto.result

import com.didit.domain.retrospect.enums.RetroStatus
import java.util.UUID

data class RetrospectiveResult(
    val retrospectiveId: UUID,
    val status: RetroStatus,
    val summary: RetrospectiveSummaryResult?
)