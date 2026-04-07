package com.didit.application.organization.required

import com.didit.domain.organization.Project
import org.springframework.data.repository.Repository
import java.util.UUID

interface ProjectRepository : Repository<Project, UUID> {
    fun save(project: Project): Project

    fun existsByUserIdAndNameAndDeletedAtIsNull(
        userId: UUID,
        name: String,
    ): Boolean

    fun findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId: UUID): List<Project>

    fun findByIdAndUserIdAndDeletedAtIsNull(
        id: UUID,
        userId: UUID,
    ): Project?

    fun findByIdAndDeletedAtIsNull(id: UUID): Project?
}
