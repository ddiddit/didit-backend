package com.didit.application.organization.provided

import java.util.UUID

interface ProjectModifier {
    fun updateName(
        userId: UUID,
        projectId: UUID,
        newName: String,
    )
}
