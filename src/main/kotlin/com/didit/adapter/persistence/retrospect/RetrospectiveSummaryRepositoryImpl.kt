package com.didit.adapter.persistence.retrospect

import com.didit.application.retrospect.required.RetrospectiveSummaryRepository
import com.didit.domain.retrospect.RetrospectiveSummary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RetrospectiveSummaryRepositoryImpl(
    private val springDataRepository: RetrospectiveSummarySpringDataRepository,
) : RetrospectiveSummaryRepository {
    override fun save(summary: RetrospectiveSummary): RetrospectiveSummary = springDataRepository.save(summary)

    override fun findByRetrospectiveId(retrospectiveId: UUID): RetrospectiveSummary? =
        springDataRepository.findByRetrospectiveId(retrospectiveId)
}

interface RetrospectiveSummarySpringDataRepository : JpaRepository<RetrospectiveSummary, UUID> {
    fun findByRetrospectiveId(retrospectiveId: UUID): RetrospectiveSummary?
}
