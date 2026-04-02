package com.didit.application.project.required

import com.didit.domain.project.Project
import org.springframework.data.repository.Repository
import java.util.UUID

interface ProjectRepository : Repository<Project, UUID> {
    fun save(project: Project): Project

    fun existsByUserIdAndNameAndDeletedAtIsNull(
        userId: UUID,
        name: String,
    ): Boolean
}
