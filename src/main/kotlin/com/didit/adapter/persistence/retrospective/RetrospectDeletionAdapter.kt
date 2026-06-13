package com.didit.adapter.persistence.retrospective

import com.didit.application.organization.required.RetrospectTagRepository
import com.didit.application.retrospect.provided.RetrospectDeletionPort
import com.didit.application.retrospect.required.RetrospectiveRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RetrospectDeletionAdapter(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val retrospectiveTagRepository: RetrospectTagRepository,
) : RetrospectDeletionPort {
    override fun deleteByUserId(userId: UUID) {
        retrospectiveRepository.findAllByUserId(userId).forEach { retrospective ->
            retrospectiveTagRepository.deleteAllByRetrospectiveId(retrospective.id)
            retrospectiveRepository.delete(retrospective)
        }
    }
}
