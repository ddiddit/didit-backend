package com.didit.application.organization

import com.didit.application.organization.provided.ProjectFinder
import com.didit.application.organization.required.ProjectRepository
import com.didit.domain.organization.Project
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class ProjectFinderService(
    private val projectRepository: ProjectRepository,
) : ProjectFinder {
    override fun findAllByUserId(userId: UUID): List<Project> = projectRepository.findAllForUser(userId)
}
