package com.didit.application.organization.provided

import com.didit.domain.retrospect.Retrospective
import java.util.UUID

interface RetrospectTagFinder {
    fun findAllByTagId(tagId: UUID): List<Retrospective>
}
