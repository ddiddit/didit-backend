package com.didit.domain.retrospect

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class SearchHistoryTest {
    private val userId = UUID.randomUUID()

    @Test
    fun `create - 검색 기록을 생성한다`() {
        val keyword = "회고"

        val history = SearchHistory.create(userId, keyword)

        assertThat(history.userId).isEqualTo(userId)
        assertThat(history.keyword).isEqualTo(keyword)
        assertThat(history.searchedAt).isNotNull
        assertThat(history.id).isNotNull
    }

    @Test
    fun `updateSearchedAt - 검색 시간을 갱신한다`() {
        val history = SearchHistory.create(userId, "회고")
        val before = history.searchedAt

        Thread.sleep(10)

        history.updateSearchedAt()

        assertThat(history.searchedAt).isAfter(before)
    }
}
