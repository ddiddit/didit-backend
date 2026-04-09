package com.didit.application.organization

import com.didit.application.organization.exception.TagNotFoundException
import com.didit.application.organization.required.TagRepository
import com.didit.domain.organization.Tag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class TagModifyServiceTest {
    @Mock
    lateinit var tagRepository: TagRepository

    @InjectMocks
    lateinit var tagModifyService: TagModifyService

    private val userId = UUID.randomUUID()

    @Test
    fun `태그 삭제 성공`() {
        val tagId = UUID.randomUUID()
        val tag = Tag.create(userId, "테스트 태그")

        whenever(tagRepository.findByIdAndUserIdAndDeletedAtIsNull(tagId, userId))
            .thenReturn(tag)

        tagModifyService.delete(userId, tagId)

        assertThat(tag.delete()).isNotNull()
        verify(tagRepository).findByIdAndUserIdAndDeletedAtIsNull(tagId, userId)
    }

    @Test
    fun `태그가 존재하지 않으면 예외 발생`() {
        val tagId = UUID.randomUUID()

        whenever(tagRepository.findByIdAndUserIdAndDeletedAtIsNull(tagId, userId))
            .thenReturn(null)

        assertThrows<TagNotFoundException> {
            tagModifyService.delete(userId, tagId)
        }
    }
}
