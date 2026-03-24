package com.didit.adapter.retrospect.out.persistence.repository

import com.didit.adapter.retrospect.out.persistence.entity.RetrospectiveJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.EntityGraph
import java.util.UUID

interface RetrospectiveJpaRepository : JpaRepository<RetrospectiveJpaEntity, UUID> {
    @EntityGraph(attributePaths = ["chatMessages"])
    fun findWithChatMessagesById(id: UUID): RetrospectiveJpaEntity?
}
