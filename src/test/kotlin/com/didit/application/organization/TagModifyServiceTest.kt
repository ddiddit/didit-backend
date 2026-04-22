package com.didit.application.organization

import com.didit.application.organization.exception.TagNotFoundException
import com.didit.application.organization.required.RetrospectTagRepository
import com.didit.application.organization.required.TagRepository
import com.didit.domain.organization.RetrospectiveTag
import com.didit.domain.organization.Tag
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class TagModifyServiceTest {
    @Mock
    lateinit var tagRepository: TagRepository

    @Mock
    lateinit var retrospectTagRepository: RetrospectTagRepository

    @InjectMocks
    lateinit var tagModifyService: TagModifyService

    private val userId = UUID.randomUUID()

    @Test
    fun `태그 삭제 성공`() {
        val tagId = UUID.randomUUID()
        val retrospectId = UUID.randomUUID()

        val tag = spy(Tag.create(userId, "테스트 태그"))

        val retrospectTag1 = spy(RetrospectiveTag.add(retrospectId, tagId))
        val retrospectTag2 = spy(RetrospectiveTag.add(retrospectId, tagId))

        whenever(tagRepository.findByIdAndUserIdAndDeletedAtIsNull(tagId, userId))
            .thenReturn(tag)

        whenever(retrospectTagRepository.findAllByTagIdAndDeletedAtIsNull(tagId))
            .thenReturn(listOf(retrospectTag1, retrospectTag2))

        tagModifyService.delete(userId, tagId)

        verify(tagRepository).findByIdAndUserIdAndDeletedAtIsNull(tagId, userId)
        verify(retrospectTagRepository).findAllByTagIdAndDeletedAtIsNull(tagId)

        verify(retrospectTag1).delete()
        verify(retrospectTag2).delete()
        verify(tag).delete()
    }

    @Test
    fun `태그가 존재하지 않으면 예외 발생`() {
        val tagId = UUID.randomUUID()

        whenever(tagRepository.findByIdAndUserIdAndDeletedAtIsNull(tagId, userId))
            .thenReturn(null)

        assertThrows<TagNotFoundException> {
            tagModifyService.delete(userId, tagId)
        }

        verify(tagRepository).findByIdAndUserIdAndDeletedAtIsNull(tagId, userId)
        verifyNoInteractions(retrospectTagRepository)
    }
}
