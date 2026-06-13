package com.didit.adapter.persistence.organization

import com.didit.application.organization.provided.OrganizationDeletionPort
import com.didit.application.organization.required.ProjectRepository
import com.didit.application.organization.required.TagRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrganizationDeletionAdapter(
    private val projectRepository: ProjectRepository,
    private val tagRepository: TagRepository,
) : OrganizationDeletionPort {
    override fun deleteByUserId(userId: UUID) {
        projectRepository.deleteAllByUserId(userId)
        tagRepository.deleteAllByUserId(userId)
    }
}
