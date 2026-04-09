package com.didit.application.organization.provided

import com.didit.domain.organization.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class TagRegisterTest {
    @Mock
    lateinit var tagRegister: TagRegister

    @Test
    fun `create 호출 및 반환 검증`() {
        val userId = UUID.randomUUID()
        val tagName = "테스트 태그"
        val tag = Tag.create(userId, tagName)

        whenever(tagRegister.create(userId, tagName)).thenReturn(tag)

        val result = tagRegister.create(userId, tagName)

        verify(tagRegister).create(userId, tagName)

        assertEquals(tag, result)
        assertEquals(tagName, result.name)
        assertEquals(userId, result.userId)
    }
}
