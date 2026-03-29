package com.didit.application.retrospect.required

import com.didit.domain.retrospect.SearchHistory
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.Test

class SearchHistoryRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var searchHistoryRepository: SearchHistoryRepository

    private val userId = UUID.randomUUID()

    @Test
    fun `save`() {
        val history = SearchHistory.create(userId, "회고")

        searchHistoryRepository.save(history)

        val found = searchHistoryRepository.findAllByUserIdOrderBySearchedAtDesc(userId)

        assertThat(found).hasSize(1)
        assertThat(found[0].keyword).isEqualTo("회고")
    }

    @Test
    fun `findByUserIdAndKeyword - 존재하는 경우`() {
        val history = SearchHistory.create(userId, "회고")
        searchHistoryRepository.save(history)

        val found = searchHistoryRepository.findByUserIdAndKeyword(userId, "회고")

        assertThat(found).isNotNull
        assertThat(found!!.keyword).isEqualTo("회고")
    }

    @Test
    fun `findByUserIdAndKeyword - 존재하지 않으면 null 반환`() {
        val found = searchHistoryRepository.findByUserIdAndKeyword(userId, "없는키워드")

        assertThat(found).isNull()
    }

    @Test
    fun `findTop10ByUserIdOrderBySearchedAtDesc - 최근 10개만 반환한다`() {
        val histories =
            (1..15).map {
                SearchHistory.create(userId, "keyword$it").apply {
                    updateSearchedAt()
                }
            }

        histories.forEach { searchHistoryRepository.save(it) }

        val result = searchHistoryRepository.findTop10ByUserIdOrderBySearchedAtDesc(userId)

        assertThat(result).hasSize(10)
    }

    @Test
    fun `findTop10ByUserIdOrderBySearchedAtDesc - 최신순으로 정렬된다`() {
        val history1 = SearchHistory.create(userId, "old")
        val history2 = SearchHistory.create(userId, "new")

        searchHistoryRepository.save(history1)
        Thread.sleep(10)
        searchHistoryRepository.save(history2)

        val result = searchHistoryRepository.findTop10ByUserIdOrderBySearchedAtDesc(userId)

        assertThat(result[0].keyword).isEqualTo("new")
        assertThat(result[1].keyword).isEqualTo("old")
    }

    @Test
    fun `findAllByUserIdOrderBySearchedAtDesc - 전체 조회 및 정렬`() {
        val history1 = SearchHistory.create(userId, "A")
        val history2 = SearchHistory.create(userId, "B")

        searchHistoryRepository.save(history1)
        Thread.sleep(10)
        searchHistoryRepository.save(history2)

        val result = searchHistoryRepository.findAllByUserIdOrderBySearchedAtDesc(userId)

        assertThat(result).hasSize(2)
        assertThat(result[0].keyword).isEqualTo("A")
        assertThat(result[1].keyword).isEqualTo("B")
    }

    @Test
    fun `deleteAll - 여러 검색 기록을 삭제한다`() {
        val histories =
            listOf(
                SearchHistory.create(userId, "회고"),
                SearchHistory.create(userId, "테스트"),
            )

        histories.forEach { searchHistoryRepository.save(it) }

        searchHistoryRepository.deleteAll(histories)

        val result = searchHistoryRepository.findAllByUserIdOrderBySearchedAtDesc(userId)

        assertThat(result).isEmpty()
    }
}
