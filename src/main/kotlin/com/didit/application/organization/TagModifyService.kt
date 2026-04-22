package com.didit.application.organization

import com.didit.application.organization.exception.TagNotFoundException
import com.didit.application.organization.provided.TagModifier
import com.didit.application.organization.required.RetrospectTagRepository
import com.didit.application.organization.required.TagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class TagModifyService(
    private val tagRepository: TagRepository,
    private val retrospectTagRepository: RetrospectTagRepository,
) : TagModifier {
    @Transactional
    override fun delete(
        userId: UUID,
        tagId: UUID,
    ) {
        val tag =
            tagRepository.findByIdAndUserIdAndDeletedAtIsNull(tagId, userId)
                ?: throw TagNotFoundException(tagId)

        val retrospectTags = retrospectTagRepository.findAllByTagIdAndDeletedAtIsNull(tagId)

        retrospectTags.forEach { it.delete() }

        tag.delete()
    }
}
