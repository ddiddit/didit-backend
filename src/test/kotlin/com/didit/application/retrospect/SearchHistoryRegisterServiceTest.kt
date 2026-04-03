package com.didit.application.retrospect

import com.didit.application.retrospect.required.SearchHistoryRepository
import com.didit.domain.retrospect.SearchHistory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class SearchHistoryRegisterServiceTest {
    @Mock
    lateinit var searchHistoryRepository: SearchHistoryRepository

    @InjectMocks
    lateinit var searchHistoryRegisterService: SearchHistoryRegisterService

    private val userId = UUID.randomUUID()

    @Test
    fun `기존 검색 기록이 없으면 새로 저장한다`() {
        val keyword = "회고"

        whenever(searchHistoryRepository.findByUserIdAndKeyword(userId, keyword))
            .thenReturn(null)

        whenever(searchHistoryRepository.findAllByUserIdOrderBySearchedAtDesc(userId))
            .thenReturn(emptyList())

        searchHistoryRegisterService.register(userId, keyword)

        verify(searchHistoryRepository).save(any())
    }

    @Test
    fun `기존 검색 기록이 있으면 searchedAt을 업데이트한다`() {
        val keyword = "회고"
        val history = SearchHistory.create(userId, keyword)

        whenever(searchHistoryRepository.findByUserIdAndKeyword(userId, keyword))
            .thenReturn(history)

        whenever(searchHistoryRepository.findAllByUserIdOrderBySearchedAtDesc(userId))
            .thenReturn(listOf(history))

        val before = history.searchedAt

        searchHistoryRegisterService.register(userId, keyword)

        verify(searchHistoryRepository, never()).save(any())
        assertThat(history.searchedAt).isAfterOrEqualTo(before)
    }

    @Test
    fun `검색 기록이 10개를 초과하면 초과된 데이터를 삭제한다`() {
        val keyword = "회고"

        val histories =
            (1..11).map {
                SearchHistory.create(userId, "keyword$it")
            }

        whenever(searchHistoryRepository.findByUserIdAndKeyword(userId, keyword))
            .thenReturn(null)

        whenever(searchHistoryRepository.findAllByUserIdOrderBySearchedAtDesc(userId))
            .thenReturn(histories)

        searchHistoryRegisterService.register(userId, keyword)

        verify(searchHistoryRepository).deleteAll(any())
    }

    @Test
    fun `검색 기록이 10개 이하이면 삭제하지 않는다`() {
        val keyword = "회고"

        val histories =
            (1..5).map {
                SearchHistory.create(userId, "keyword$it")
            }
        whenever(searchHistoryRepository.findByUserIdAndKeyword(userId, keyword))
            .thenReturn(null)
        whenever(searchHistoryRepository.findAllByUserIdOrderBySearchedAtDesc(userId))
            .thenReturn(histories)

        searchHistoryRegisterService.register(userId, keyword)

        verify(searchHistoryRepository, never()).deleteAll(any())
    }
}
