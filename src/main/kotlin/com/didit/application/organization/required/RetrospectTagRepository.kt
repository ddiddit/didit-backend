package com.didit.application.organization.required

import com.didit.domain.organization.RetrospectiveTag
import org.springframework.data.repository.Repository
import java.util.UUID

interface RetrospectTagRepository : Repository<RetrospectiveTag, UUID> {
    fun save(retrospectTag: RetrospectiveTag): RetrospectiveTag

    fun countByRetrospectiveIdAndIsActiveIsTrue(retrospectiveId: UUID): Int

    fun findByRetrospectiveIdAndTagIdAndDeletedAtIsNull(
        retrospectiveId: UUID,
        tagId: UUID,
    ): RetrospectiveTag?

    fun findByRetrospectiveIdAndTagId(
        retrospectiveId: UUID,
        tagId: UUID,
    ): RetrospectiveTag?

    fun findAllByTagIdAndDeletedAtIsNull(tagId: UUID): List<RetrospectiveTag>

    fun findAllByRetrospectiveIdAndIsActiveTrueAndDeletedAtIsNull(retrospectiveId: UUID): List<RetrospectiveTag>
}
