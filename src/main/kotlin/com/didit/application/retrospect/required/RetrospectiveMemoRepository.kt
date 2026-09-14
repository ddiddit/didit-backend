package com.didit.application.retrospect.required

import com.didit.domain.retrospect.RetrospectiveMemo
import org.springframework.data.repository.Repository
import java.time.LocalDate
import java.util.UUID

interface RetrospectiveMemoRepository : Repository<RetrospectiveMemo, UUID> {
    fun save(memo: RetrospectiveMemo): RetrospectiveMemo

    fun existsByRetrospectiveIdAndMemoDate(
        retrospectiveId: UUID,
        memoDate: LocalDate,
    ): Boolean

    fun findAllByRetrospectiveIdOrderByMemoDateDesc(retrospectiveId: UUID): List<RetrospectiveMemo>

    fun findByIdAndRetrospectiveId(
        id: UUID,
        retrospectiveId: UUID,
    ): RetrospectiveMemo?

    fun delete(memo: RetrospectiveMemo)

    fun deleteAllByRetrospectiveId(retrospectiveId: UUID)
}
