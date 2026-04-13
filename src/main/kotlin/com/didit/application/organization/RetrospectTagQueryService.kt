package com.didit.application.organization

import com.didit.application.organization.exception.TagNotFoundException
import com.didit.application.organization.provided.RetrospectTagFinder
import com.didit.application.organization.required.RetrospectTagRepository
import com.didit.application.organization.required.TagRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.Retrospective
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrospectTagQueryService(
    private val retrospectTagRepository: RetrospectTagRepository,
    private val retrospectiveRepository: RetrospectiveRepository,
    private val tagRepository: TagRepository,
) : RetrospectTagFinder {
    override fun findAllByTagId(tagId: UUID): List<Retrospective> {
        val tag =
            tagRepository.findByIdAndDeletedAtIsNull(tagId)
                ?: throw TagNotFoundException(tagId)

        val retrospectiveIds = retrospectTagRepository.findRetrospectiveIdsByTagIdAndDeletedAtIsNull(tag.id)

        return retrospectiveRepository.findAllByIdInAndDeletedAtIsNull(retrospectiveIds)
    }
}
