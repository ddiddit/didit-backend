package com.didit.application.organization.required

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
class TagRepositoryTest {
    @Mock
    lateinit var tagRepository: TagRepository

    private val userId = UUID.randomUUID()

    @Test
    fun `save`() {
        val tag = createTag()

        whenever(tagRepository.save(tag)).thenReturn(tag)

        val result = tagRepository.save(tag)

        verify(tagRepository).save(tag)
        assertEquals(tag, result)
    }

    @Test
    fun `existByUserIdAndNameAndDeletedAtIsNull`() {
        val name = "테스트 태그"

        whenever(tagRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, name))
            .thenReturn(true)

        val result = tagRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, name)

        verify(tagRepository).existsByUserIdAndNameAndDeletedAtIsNull(userId, name)
        assertEquals(true, result)
    }

    @Test
    fun `findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc`() {
        val tags =
            listOf(
                createTag(),
                createTag(),
            )

        whenever(tagRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId))
            .thenReturn(tags)

        val result = tagRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)

        verify(tagRepository).findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
        assertEquals(2, result.size)
    }

    @Test
    fun `finaByIdAndUserIdAndDeletedAtIsNull`() {
        val tag = createTag()
        val tagId = tag.id

        whenever(tagRepository.findByIdAndUserIdAndDeletedAtIsNull(tagId, userId))
            .thenReturn(tag)

        val result = tagRepository.findByIdAndUserIdAndDeletedAtIsNull(tagId, userId)

        verify(tagRepository).findByIdAndUserIdAndDeletedAtIsNull(tagId, userId)
        assertEquals(tag, result)
    }

    private fun createTag(): Tag = Tag.create(userId = userId, name = "테스트 태그")
}
