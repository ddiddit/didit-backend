package com.didit.application.retrospect

import com.didit.application.retrospect.provided.SearchHistoryFinder
import com.didit.application.retrospect.required.SearchHistoryRepository
import com.didit.domain.retrospect.SearchHistory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class SearchHistoryFinderService(
    private val searchHistoryRepository: SearchHistoryRepository,
) : SearchHistoryFinder {
    override fun findRecent(userId: UUID): List<SearchHistory> = searchHistoryRepository.findTop10ByUserIdOrderBySearchedAtDesc(userId)
}
