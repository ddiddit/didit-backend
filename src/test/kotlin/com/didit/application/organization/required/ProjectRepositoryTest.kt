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

    @Test
    fun `findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc`() {
        val projects =
            listOf(
                createProject(),
                createProject(),
            )

        whenever(
            projectRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId),
        ).thenReturn(projects)

        val result =
            projectRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)

        verify(projectRepository)
            .findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)

        assertEquals(2, result.size)
    }

    @Test
    fun `findByIdAndUserIdAndDeletedAtIsNull`() {
        val project = createProject()
        val projectId = project.id

        whenever(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId))
            .thenReturn(project)

        val result = projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId)

        verify(projectRepository).findByIdAndUserIdAndDeletedAtIsNull(projectId, userId)
        assertEquals(project, result)
    }

    @Test
    fun `findByIdAndDeletedAtIsNull`() {
        val project = createProject()
        val projectId = project.id

        whenever(projectRepository.findByIdAndDeletedAtIsNull(projectId))
            .thenReturn(project)

        val result = projectRepository.findByIdAndDeletedAtIsNull(projectId)

        verify(projectRepository).findByIdAndDeletedAtIsNull(projectId)
        assertEquals(project, result)
    }

    private fun createProject(): Project =
        Project.create(
            userId = userId,
            name = "프로젝트 이름",
        )
}
