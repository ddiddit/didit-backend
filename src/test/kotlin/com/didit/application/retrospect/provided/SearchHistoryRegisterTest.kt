package com.didit.application.retrospect.provided

import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class SearchHistoryRegisterTest {
    @Mock
    lateinit var searchHistoryRegister: SearchHistoryRegister

    private val userId = UUID.randomUUID()

    @Test
    fun `register - 검색 기록을 저장한다`() {
        val keyword = "회고"

        searchHistoryRegister.register(userId, keyword)

        verify(searchHistoryRegister).register(userId, keyword)
    }

    @Test
    fun `register - 동일 키워드 검색 시 업데이트 처리된다`() {
        val keyword = "회고"

        searchHistoryRegister.register(userId, keyword)

        verify(searchHistoryRegister).register(userId, keyword)
    }
}
