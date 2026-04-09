package com.didit.application.organization

import com.didit.application.organization.provided.TagFinder
import com.didit.application.organization.required.TagRepository
import com.didit.domain.organization.Tag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class TagQueryService(
    private val tagRepository: TagRepository,
) : TagFinder {
    override fun findAllByUserId(userId: UUID): List<Tag> = tagRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
}
