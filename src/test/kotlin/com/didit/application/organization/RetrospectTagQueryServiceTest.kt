package com.didit.application.organization

import com.didit.application.organization.required.TagRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.organization.Tag
import com.didit.domain.retrospect.Retrospective
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class RetrospectTagQueryServiceTest {
    @Mock
    lateinit var retrospectiveRepository: RetrospectiveRepository

    @Mock
    lateinit var tagRepository: TagRepository

    @InjectMocks
    lateinit var retrospectTagQueryService: RetrospectTagQueryService

    private val tagId = UUID.randomUUID()

    @Test
    fun `태그로 회고 목록 조회`() {
        val tag = Tag.create(UUID.randomUUID(), "태그")

        val retrospectives =
            listOf(
                Retrospective.create(UUID.randomUUID()).apply {
                    complete("회고1")
                },
                Retrospective.create(UUID.randomUUID()).apply {
                    complete("회고2")
                },
            )

        whenever(tagRepository.findByIdAndDeletedAtIsNull(tagId))
            .thenReturn(tag)

        whenever(retrospectiveRepository.findAllByTagId(tagId))
            .thenReturn(retrospectives)

        val result = retrospectTagQueryService.findAllByTagId(tagId)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.title })
            .containsExactlyInAnyOrder("회고1", "회고2")
    }
}
