package com.didit.application.organization

import com.didit.application.organization.exception.ProjectNotFoundException
import com.didit.application.organization.provided.ProjectModifier
import com.didit.application.organization.required.ProjectRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class ProjectModifierService(
    private val projectRepository: ProjectRepository,
    private val retrospectiveRepository: RetrospectiveRepository,
) : ProjectModifier {
    @Transactional
    override fun deleteProject(
        userId: UUID,
        projectId: UUID,
    ) {
        val project =
            projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId)
                ?: throw ProjectNotFoundException(projectId)

        val retrospectives = retrospectiveRepository.findAllByProjectIdAndDeletedAtIsNull(projectId)

        retrospectives.forEach { it.detachProject() }

        project.delete()
    }
}
