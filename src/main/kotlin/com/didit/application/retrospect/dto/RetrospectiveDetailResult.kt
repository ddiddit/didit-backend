package com.didit.application.retrospect.dto

import com.didit.domain.organization.Project
import com.didit.domain.organization.Tag
import com.didit.domain.retrospect.Retrospective

data class RetrospectiveDetailResult(
    val retrospective: Retrospective,
    val project: Project?,
    val tags: List<Tag>,
)
