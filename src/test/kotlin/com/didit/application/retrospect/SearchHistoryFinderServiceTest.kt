package com.didit.application.retrospect

import com.didit.application.retrospect.required.SearchHistoryRepository
import com.didit.domain.retrospect.SearchHistory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class SearchHistoryFinderServiceTest {
    @Mock
    lateinit var searchHistoryRepository: SearchHistoryRepository

    @InjectMocks
    lateinit var searchHistoryFinderService: SearchHistoryFinderService

    private val userId = UUID.randomUUID()

    @Test
    fun `최근 검색 기록을 조회한다`() {
        val histories =
            listOf(
                SearchHistory.create(userId, "회고"),
                SearchHistory.create(userId, "테스트"),
            )

        whenever(searchHistoryRepository.findTop10ByUserIdOrderBySearchedAtDesc(userId))
            .thenReturn(histories)

        val result = searchHistoryFinderService.findRecent(userId)

        verify(searchHistoryRepository)
            .findTop10ByUserIdOrderBySearchedAtDesc(userId)

        assertThat(result).hasSize(2)
        assertThat(result[0].keyword).isEqualTo("회고")
    }

    @Test
    fun `검색 기록이 없으면 빈 리스트를 반환한다`() {
        whenever(searchHistoryRepository.findTop10ByUserIdOrderBySearchedAtDesc(userId))
            .thenReturn(emptyList())

        val result = searchHistoryFinderService.findRecent(userId)

        verify(searchHistoryRepository)
            .findTop10ByUserIdOrderBySearchedAtDesc(userId)

        assertThat(result).isEmpty()
    }
}
