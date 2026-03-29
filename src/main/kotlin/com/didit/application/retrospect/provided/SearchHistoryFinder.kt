package com.didit.application.retrospect.provided

import com.didit.domain.retrospect.SearchHistory
import java.util.UUID

interface SearchHistoryFinder {
    fun findRecent(userId: UUID): List<SearchHistory>
}
