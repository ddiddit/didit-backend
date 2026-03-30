package com.didit.application.retrospect.provided

import com.didit.domain.retrospect.SearchHistory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class SearchHistoryFinderTest {
    @Mock
    lateinit var searchHistoryFinder: SearchHistoryFinder

    private val userId = UUID.randomUUID()

    @Test
    fun `findRecent - 최근 검색 기록을 조회한다`() {
        val histories =
            listOf(
                SearchHistory.create(userId, "회고"),
                SearchHistory.create(userId, "테스트"),
            )

        whenever(searchHistoryFinder.findRecent(userId))
            .thenReturn(histories)

        val result = searchHistoryFinder.findRecent(userId)

        verify(searchHistoryFinder).findRecent(userId)
        assertThat(result).hasSize(2)
    }

    @Test
    fun `findRecent - 검색 기록이 없으면 빈 리스트를 반환한다`() {
        whenever(searchHistoryFinder.findRecent(userId))
            .thenReturn(emptyList())

        val result = searchHistoryFinder.findRecent(userId)

        verify(searchHistoryFinder).findRecent(userId)
        assertThat(result).isEmpty()
    }
}
