package com.didit.application.organization

import com.didit.application.organization.required.TagRepository
import com.didit.domain.organization.Tag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class TagQueryServiceTest {
    @Mock
    lateinit var tagRepository: TagRepository

    @InjectMocks
    lateinit var tagQueryService: TagQueryService

    private val userId = UUID.randomUUID()

    @Test
    fun `사용자 태그 목록 조회`() {
        val tags =
            listOf(
                Tag.create(userId, "태그1"),
                Tag.create(userId, "태그2"),
            )

        whenever(tagRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId))
            .thenReturn(tags)

        val result = tagQueryService.findAllByUserId(userId)

        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("태그1")
        assertThat(result[1].name).isEqualTo("태그2")
    }
}
