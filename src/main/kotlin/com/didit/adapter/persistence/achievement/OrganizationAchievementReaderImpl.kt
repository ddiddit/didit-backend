package com.didit.adapter.persistence.achievement

import com.didit.application.achievement.required.OrganizationAchievementReader
import com.didit.application.organization.required.ProjectRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrganizationAchievementReaderImpl(
    private val projectRepository: ProjectRepository,
    private val retrospectiveRepository: RetrospectiveRepository,
) : OrganizationAchievementReader {
    override fun countProjects(userId: UUID): Int = projectRepository.findAllByUserIdAndDeletedAtIsNull(userId).size

    override fun countProjectAssignedRetros(userId: UUID): Int =
        retrospectiveRepository
            .findAllCompletedByUserId(userId)
            .count { it.projectId != null }

    override fun maxRetroCountInOneProject(userId: UUID): Int =
        retrospectiveRepository
            .findAllCompletedByUserId(userId)
            .mapNotNull { it.projectId }
            .groupingBy { it }
            .eachCount()
            .values
            .maxOrNull()
            ?: 0
}
