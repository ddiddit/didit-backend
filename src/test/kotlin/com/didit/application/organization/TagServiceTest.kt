package com.didit.application.organization

import com.didit.application.organization.exception.DuplicateTagNameException
import com.didit.application.organization.required.TagRepository
import com.didit.domain.organization.Tag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class TagServiceTest {
    @Mock
    lateinit var tagRepository: TagRepository

    @InjectMocks
    lateinit var tagService: TagService

    private val userId = UUID.randomUUID()

    @Test
    fun `태그 생성 성공`() {
        val name = "테스트 태그"
        val tag = Tag.create(userId, name)

        whenever(tagRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, name))
            .thenReturn(false)
        whenever(tagRepository.save(any()))
            .thenReturn(tag)

        val result = tagService.create(userId, name)

        verify(tagRepository).save(any())

        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.name).isEqualTo(name)
    }

    @Test
    fun `태그 이름 중복 시 예외 발생`() {
        val name = "테스트 태그"

        whenever(tagRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, name))
            .thenReturn(true)

        assertThrows<DuplicateTagNameException> {
            tagService.create(userId, name)
        }
    }

    @Test
    fun `태그 이름 trim 처리`() {
        val name = "  테스트 태그  "
        val trimmed = "테스트 태그"
        val tag = Tag.create(userId, trimmed)

        whenever(tagRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, trimmed))
            .thenReturn(false)
        whenever(tagRepository.save(any()))
            .thenAnswer { it.arguments[0] }

        val result = tagService.create(userId, name)

        assertThat(result.name).isEqualTo(tag.name)
    }
}
