package com.didit.application.retrospect.required

import com.didit.domain.retrospect.RetrospectiveSummary
import java.util.UUID

interface RetrospectiveSummaryRepository {
    fun save(summary: RetrospectiveSummary): RetrospectiveSummary

    fun findByRetrospectiveId(retrospectiveId: UUID): RetrospectiveSummary?
}
