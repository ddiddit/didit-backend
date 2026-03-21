package com.didit.application.retrospect.port.out

import com.didit.domain.retrospect.entity.Retrospective
import java.util.UUID

interface RetrospectiveQueryPort {
    fun findById(retrospectiveId: UUID): Retrospective?
}