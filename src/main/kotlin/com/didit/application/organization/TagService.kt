package com.didit.application.organization

import com.didit.application.organization.exception.DuplicateTagNameException
import com.didit.application.organization.provided.TagRegister
import com.didit.application.organization.required.TagRepository
import com.didit.domain.organization.Tag
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class TagService(
    private val tagRepository: TagRepository,
) : TagRegister {
    private val logger = LoggerFactory.getLogger(TagService::class.java)

    @Transactional
    override fun create(
        userId: UUID,
        name: String,
    ): Tag {
        val normalized = name.trim()

        if (tagRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, normalized)) {
            throw DuplicateTagNameException(
                userId,
                normalized,
            )
        }

        val tag = Tag.create(userId, normalized)
        val saved = tagRepository.save(tag)

        logger.info("태그 생성 성공: userId:$userId, tagId: ${tag.id}, tagName:${tag.name}")

        return saved
    }
}
