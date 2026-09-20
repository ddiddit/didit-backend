package com.didit.application.retrospect.required

import com.didit.domain.retrospect.RetrospectiveFeedback
import org.springframework.data.repository.Repository
import java.util.UUID

interface RetrospectiveFeedbackRepository : Repository<RetrospectiveFeedback, UUID> {
    fun save(feedback: RetrospectiveFeedback): RetrospectiveFeedback

    fun flush()

    fun findByRetrospectiveId(retrospectiveId: UUID): RetrospectiveFeedback?
}
