package com.didit.application.organization.required

import com.didit.domain.organization.Project
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ProjectRepositoryTest {
    @Mock
    lateinit var projectRepository: ProjectRepository

    private val userId = UUID.randomUUID()

    @Test
    fun `save`() {
        val project = createProject()

        whenever(projectRepository.save(project)).thenReturn(project)

        val result = projectRepository.save(project)

        verify(projectRepository).save(project)
        assertEquals(project, result)
    }

    @Test
    fun `existsByUserIdAndNameAndDeletedAtIsNull`() {
        val name = "프로젝트 이름"

        whenever(
            projectRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, name),
        ).thenReturn(true)

        val result =
            projectRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, name)

        verify(projectRepository)
            .existsByUserIdAndNameAndDeletedAtIsNull(userId, name)

        assertEquals(true, result)
    }

    private fun createProject(): Project =
        Project.create(
            userId = userId,
            name = "프로젝트 이름",
        )
}
