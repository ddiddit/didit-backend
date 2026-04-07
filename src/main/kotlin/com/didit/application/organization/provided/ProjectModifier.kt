package com.didit.application.organization.provided

import java.util.UUID

interface ProjectModifier {
    fun deleteProject(
        userId: UUID,
        projectId: UUID,
    )
}
