package com.didit.application.retrospect.required

import com.didit.domain.retrospect.SearchHistory
import org.springframework.data.repository.Repository
import java.util.UUID

interface SearchHistoryRepository : Repository<SearchHistory, UUID> {
    fun save(history: SearchHistory)

    fun findTop10ByUserIdOrderBySearchedAtDesc(userId: UUID): List<SearchHistory>

    fun findAllByUserIdOrderBySearchedAtDesc(userId: UUID): List<SearchHistory>

    fun findByUserIdAndKeyword(
        userId: UUID,
        keyword: String,
    ): SearchHistory?

    fun deleteAll(histories: Iterable<SearchHistory>)
}
