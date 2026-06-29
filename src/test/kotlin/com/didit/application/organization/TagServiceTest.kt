package com.didit.application.organization

import com.didit.application.organization.exception.DuplicateTagNameException
import com.didit.application.organization.exception.InvalidTagNameException
import com.didit.application.organization.exception.OrganizationErrorCode
import com.didit.application.organization.required.TagRepository
import com.didit.domain.organization.Tag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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
    fun `태그 이름 trim 처리 후 10자 까지 생성 가능`() {
        val name = "  1234567890  "
        val trimmed = "1234567890"
        val tag = Tag.create(userId, trimmed)

        whenever(tagRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, trimmed))
            .thenReturn(false)
        whenever(tagRepository.save(any()))
            .thenAnswer { it.arguments[0] }

        val result = tagService.create(userId, name)

        assertThat(result.name).isEqualTo(tag.name)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "    ",
            "12345678901",
        ],
    )
    fun `태그 이름이 공백이거나 10자 초과 시 예외`(name: String) {
        val exception =
            assertThrows<InvalidTagNameException> {
                tagService.create(userId, name)
            }

        assertThat(exception.errorCode).isEqualTo(OrganizationErrorCode.INVALID_TAG_NAME)
        assertThat(exception.message).isEqualTo("태그명은 비어 있을 수 없으며 10자 이하여야 합니다.")

        verifyNoInteractions(tagRepository)
    }
}
