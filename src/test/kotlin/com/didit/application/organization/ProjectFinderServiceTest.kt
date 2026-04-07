package com.didit.application.organization

import com.didit.application.organization.required.ProjectRepository
import com.didit.domain.organization.Project
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class ProjectFinderServiceTest {
    @Mock
    lateinit var projectRepository: ProjectRepository

    @InjectMocks
    lateinit var projectFinderService: ProjectFinderService

    @Test
    fun `유저 프로젝트 목록 조회`() {
        val userId = UUID.randomUUID()

        val projects =
            listOf(
                Project.create(userId, "프로젝트1"),
                Project.create(userId, "프로젝트2"),
            )

        whenever(
            projectRepository.findAllForUser(userId),
        ).thenReturn(projects)

        val result = projectFinderService.findAllByUserId(userId)

        verify(projectRepository)
            .findAllForUser(userId)

        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("프로젝트1")
        assertThat(result[1].name).isEqualTo("프로젝트2")
    }

    @Test
    fun `프로젝트가 없으면 빈 리스트 반환`() {
        val userId = UUID.randomUUID()

        whenever(
            projectRepository.findAllForUser(userId),
        ).thenReturn(emptyList())

        val result = projectFinderService.findAllByUserId(userId)

        assertThat(result).isEmpty()
    }
}
