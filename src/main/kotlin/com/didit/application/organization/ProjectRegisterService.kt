package com.didit.application.organization

import com.didit.application.auth.provided.UserFinder
import com.didit.application.organization.exception.DuplicateProjectNameException
import com.didit.application.organization.provided.ProjectRegister
import com.didit.application.organization.required.ProjectRepository
import com.didit.domain.organization.Project
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
            Project.Companion.create(
                userId = userId,
                name = normalizedName,
            )

        return projectRepository.save(project)
    }
}
