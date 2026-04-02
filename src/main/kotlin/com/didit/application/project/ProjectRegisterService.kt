package com.didit.application.project

import com.didit.application.auth.provided.UserFinder
import com.didit.application.project.exception.DuplicateProjectNameException
import com.didit.application.project.provided.ProjectRegister
import com.didit.application.project.required.ProjectRepository
import com.didit.domain.project.Project
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class ProjectRegisterService(
    private val projectRepository: ProjectRepository,
    private val userFinder: UserFinder,
) : ProjectRegister {
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

        val project =
            Project.create(
                userId = userId,
                name = normalizedName,
            )

        return projectRepository.save(project)
    }
}
