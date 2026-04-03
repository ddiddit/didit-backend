package com.didit.application.retrospect

import com.didit.application.retrospect.provided.SearchHistoryRegister
import com.didit.application.retrospect.required.SearchHistoryRepository
import com.didit.domain.retrospect.SearchHistory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class SearchHistoryRegisterService(
    private val searchHistoryRepository: SearchHistoryRepository,
) : SearchHistoryRegister {
    companion object {
        private val logger = LoggerFactory.getLogger(SearchHistoryRegisterService::class.java)
    }

    @Transactional
    override fun register(
        userId: UUID,
        keyword: String,
    ) {
        searchHistoryRepository
            .findByUserIdAndKeyword(userId, keyword)
            ?.also {
                it.updateSearchedAt()
                logger.debug("검색 기록 갱신 - userId: $userId, keyword: $keyword")
            }
            ?: searchHistoryRepository
                .save(SearchHistory.create(userId, keyword))
                .also { logger.debug("검색 기록 저장 - userId: $userId, keyword: $keyword") }

        trimToLimit(userId)
    }

    private fun trimToLimit(userId: UUID) {
        val all = searchHistoryRepository.findAllByUserIdOrderBySearchedAtDesc(userId)

        if (all.size <= 10) return

        val toDelete = all.drop(10)

        searchHistoryRepository.deleteAll(toDelete)

        logger.debug("검색 기록 정리 - userId: $userId, 삭제 건수: ${toDelete.size}")
    }
}
