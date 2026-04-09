package com.didit.application.organization

import com.didit.application.organization.exception.TagNotFoundException
import com.didit.application.organization.provided.RetrospectTagRegister
import com.didit.application.organization.required.RetrospectTagRepository
import com.didit.application.organization.required.TagRepository
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.organization.RetrospectiveTag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrospectTagService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val retrospectTagRepository: RetrospectTagRepository,
    private val tagRepository: TagRepository,
) : RetrospectTagRegister {
    @Transactional
    override fun addTag(
        userId: UUID,
        retrospectiveId: UUID,
        tagId: UUID,
    ) {
        val retrospect =
            retrospectiveRepository.findByIdAndDeletedAtIsNull(retrospectiveId)
                ?: throw RetrospectiveNotFoundException(retrospectiveId)
        require(retrospect.userId == userId) { "해당 회고의 권한이 없습니다." }

        val tag =
            tagRepository.findByIdAndDeletedAtIsNull(tagId)
                ?: throw TagNotFoundException(tagId)

        require(tag.userId == userId) { "해당 태그의 권한이 없습니다." }

        val count = retrospectTagRepository.countByRetrospectiveIdAndDeletedAtIsNull(retrospectiveId)
        require(count < 2) { "태그는 최대 2개까지 추가 가능합니다." }

        val exists = retrospectTagRepository.existsByRetrospectiveIdAndTagIdAndDeletedAtIsNull(retrospectiveId, tagId)
        require(!exists) { "이미 추가된 태그입니다." }

        retrospectTagRepository.save(
            RetrospectiveTag.add(retrospectiveId, tagId),
        )
    }
}
