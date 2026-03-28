package com.didit.adapter.persistence.retrospect

import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.Retrospective
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RetrospectiveRepositoryImpl(
    private val springDataRepository: RetrospectiveSpringDataRepository,
) : RetrospectiveRepository {
    override fun save(retrospective: Retrospective): Retrospective = springDataRepository.save(retrospective)

    override fun findById(id: UUID): Retrospective? = springDataRepository.findByIdOrNull(id)

    override fun findByUserId(userId: UUID): Retrospective? = springDataRepository.findByUserId(userId)

    override fun findByUserIdWithChatMessages(userId: UUID): Retrospective? = springDataRepository.findByUserIdWithChatMessages(userId)
}

interface RetrospectiveSpringDataRepository : JpaRepository<Retrospective, UUID> {
    fun findByUserId(userId: UUID): Retrospective?

    @Query("SELECT r FROM Retrospective r LEFT JOIN FETCH r.chatMessages WHERE r.userId = :userId")
    fun findByUserIdWithChatMessages(userId: UUID): Retrospective?
}
