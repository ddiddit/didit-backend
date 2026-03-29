package com.didit.adapter.webapi.retrospect.dto

import com.didit.domain.retrospect.SearchHistory

data class SearchHistoryResponse(
    val keyword: String,
) {
    companion object {
        fun from(searchHistory: SearchHistory): SearchHistoryResponse =
            SearchHistoryResponse(
                keyword = searchHistory.keyword,
            )
    }
}
