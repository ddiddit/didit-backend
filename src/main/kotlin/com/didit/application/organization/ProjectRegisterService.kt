package com.didit.application.organization

import com.didit.application.auth.provided.UserFinder
import com.didit.application.organization.exception.DuplicateProjectNameException
import com.didit.application.organization.exception.ProjectNotFoundException
import com.didit.application.organization.provided.ProjectRegister
import com.didit.application.organization.required.ProjectRepository
import com.didit.domain.organization.Project
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class ProjectRegisterService(
    private val projectRepository: ProjectRepository,
    private val userFinder: UserFinder,
) : ProjectRegister {
    companion object {
        private val logger = LoggerFactory.getLogger(ProjectRegisterService::class.java)
    }

    @Transactional
    override fun create(
        userId: UUID,
        name: String,
    ): Project {
        userFinder.findByIdOrThrow(userId)

        val normalizedName = name.trim()

        if (projectRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, normalizedName)) {
            throw DuplicateProjectNameException(userId, normalizedName)
        }

        val maxOrder = projectRepository.findMaxDisplayOrder(userId)

        val displayOrder =
            if (maxOrder == null) {
                null
            } else {
                maxOrder + 1
            }
        val project =
            Project.create(
                userId = userId,
                name = normalizedName,
                displayOrder = displayOrder,
            )
        val saved = projectRepository.save(project)

        logger.info("프로젝트 생성 완료 - userId: $userId, projectName: $normalizedName, order: $displayOrder")

        return saved
    }

    @Transactional
    override fun reorder(
        userId: UUID,
        projectIds: List<UUID>,
    ) {
        val projects = projectRepository.findAllByUserIdAndDeletedAtIsNull(userId)

        require(projectIds.size == projects.size) { "잘못된 요청입니다." }
        require(projectIds.distinct().size == projectIds.size) { "중복된 ID가 있습니다." }

        val projectMap = projects.associateBy { it.id }

        for (index in projectIds.indices) {
            val id = projectIds[index]
            val project =
                projectMap[id]
                    ?: throw ProjectNotFoundException(id)

            project.updateOrder(index + 1)

            logger.info("프로젝트 순서 변경 완료 - userId: $userId, orderedIds: $projectIds")
        }
    }
}
