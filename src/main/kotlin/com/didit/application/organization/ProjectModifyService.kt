package com.didit.application.organization

import com.didit.application.organization.exception.DuplicateProjectNameException
import com.didit.application.organization.exception.ProjectNotFoundException
import com.didit.application.organization.provided.ProjectModifier
import com.didit.application.organization.required.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class ProjectModifyService(
    private val projectRepository: ProjectRepository,
) : ProjectModifier {
    @Transactional
    override fun updateName(
        userId: UUID,
        projectId: UUID,
        newName: String,
    ) {
        val project =
            projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId)
                ?: throw ProjectNotFoundException(projectId)

        val normalizedName = newName.trim()

        if (project.name == normalizedName) return

        val exist = projectRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, normalizedName)
        if (exist) throw DuplicateProjectNameException(userId, normalizedName)

        project.updateName(newName)
    }
}
