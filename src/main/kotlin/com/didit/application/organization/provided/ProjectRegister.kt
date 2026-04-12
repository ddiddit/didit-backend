package com.didit.application.organization.provided

import com.didit.domain.organization.Project
import java.util.UUID

interface ProjectRegister {
    fun create(
        userId: UUID,
        name: String,
    ): Project

    fun reorder(
        userId: UUID,
        projectIds: List<UUID>,
    )
}
