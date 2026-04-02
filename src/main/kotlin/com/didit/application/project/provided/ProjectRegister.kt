package com.didit.application.project.provided

import com.didit.domain.project.Project
import java.util.UUID

interface ProjectRegister {
    fun create(
        userId: UUID,
        name: String,
    ): Project
}
