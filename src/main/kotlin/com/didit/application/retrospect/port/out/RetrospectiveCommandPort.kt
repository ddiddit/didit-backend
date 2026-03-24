package com.didit.application.retrospect.port.out

import com.didit.domain.retrospect.entity.Retrospective

interface RetrospectiveCommandPort {
    fun save(retrospective: Retrospective): Retrospective
}
