package com.didit.application.organization.provided

import com.didit.domain.organization.Project
import java.util.UUID

interface ProjectFinder {
    fun findAllByUserId(userId: UUID): List<Project>
}
