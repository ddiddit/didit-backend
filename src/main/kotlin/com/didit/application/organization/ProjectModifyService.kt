package com.didit.application.organization

import com.didit.application.organization.exception.DuplicateProjectNameException
import com.didit.application.organization.exception.ProjectNotFoundException
import com.didit.application.organization.provided.ProjectModifier
import com.didit.application.organization.required.ProjectRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class ProjectModifyService(
    private val projectRepository: ProjectRepository,
) : ProjectModifier {
    companion object {
        private val logger = LoggerFactory.getLogger(ProjectModifyService::class.java)
    }

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

        logger.info("프로젝트 이름 수정 완료 - userId: $userId, projectId: $projectId, projectName: $normalizedName")
    }
}
