package com.didit.application.organization.required

import com.didit.domain.organization.Tag
import org.springframework.data.repository.Repository
import java.util.UUID

interface TagRepository : Repository<Tag, UUID> {
    fun save(tag: Tag): Tag

    fun existsByUserIdAndNameAndDeletedAtIsNull(
        userId: UUID,
        name: String,
    ): Boolean

    fun findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId: UUID): List<Tag>

    fun findByIdAndUserIdAndDeletedAtIsNull(
        id: UUID,
        userId: UUID,
    ): Tag?
}
