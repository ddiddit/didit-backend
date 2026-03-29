package com.didit.application.retrospect

import com.didit.application.retrospect.provided.SearchHistoryRegister
import com.didit.application.retrospect.required.SearchHistoryRepository
import com.didit.domain.retrospect.SearchHistory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class SearchHistoryRegisterService(
    private val searchHistoryRepository: SearchHistoryRepository,
) : SearchHistoryRegister {
    override fun register(
        userId: UUID,
        keyword: String,
    ) {
        val existing =
            searchHistoryRepository
                .findByUserIdAndKeyword(userId, keyword)

        if (existing != null) {
            existing.updateSearchedAt()
        } else {
            searchHistoryRepository.save(
                SearchHistory.create(userId, keyword),
            )
        }

        trimToLimit(userId)
    }

    private fun trimToLimit(userId: UUID) {
        val all =
            searchHistoryRepository
                .findAllByUserIdOrderBySearchedAtDesc(userId)

        if (all.size <= 10) return

        val toDelete = all.drop(10)
        searchHistoryRepository.deleteAll(toDelete)
    }
}
