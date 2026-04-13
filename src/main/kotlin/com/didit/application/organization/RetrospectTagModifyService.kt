package com.didit.application.organization

import com.didit.application.organization.exception.RetroTagNotFoundException
import com.didit.application.organization.exception.TagNotFoundException
import com.didit.application.organization.provided.RetrospectTagModifier
import com.didit.application.organization.required.RetrospectTagRepository
import com.didit.application.organization.required.TagRepository
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.required.RetrospectiveRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrospectTagModifyService(
    private val retrospectTagRepository: RetrospectTagRepository,
    private val retrospectiveRepository: RetrospectiveRepository,
    private val tagRepository: TagRepository,
) : RetrospectTagModifier {
    @Transactional
    override fun delete(
        userId: UUID,
        retrospectiveId: UUID,
        tagId: UUID,
    ) {
        val retrospect =
            retrospectiveRepository.findByIdAndDeletedAtIsNull(retrospectiveId)
                ?: throw RetrospectiveNotFoundException(retrospectiveId)

        require(retrospect.userId == userId) { "유저는 해당 회고에 권한이 없습니다." }

        tagRepository.findByIdAndDeletedAtIsNull(tagId)
            ?: throw TagNotFoundException(tagId)

        val retrospectTag =
            retrospectTagRepository.findByRetrospectiveIdAndTagIdAndDeletedAtIsNull(retrospectiveId, tagId)
                ?: throw RetroTagNotFoundException(retrospectiveId, tagId)

        retrospectTag.delete()
    }
}
