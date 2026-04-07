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

    @Test
    fun `findAllForUser`() {
        val projects =
            listOf(
                createProject(),
                createProject(),
            )

        whenever(projectRepository.findAllForUser(userId))
            .thenReturn(projects)

        val result = projectRepository.findAllForUser(userId)

        verify(projectRepository).findAllForUser(userId)
        assertEquals(2, result.size)
    }

    @Test
    fun `findMaxDisplayOrder`() {
        whenever(projectRepository.findMaxDisplayOrder(userId))
            .thenReturn(3)

        val result = projectRepository.findMaxDisplayOrder(userId)

        verify(projectRepository).findMaxDisplayOrder(userId)
        assertEquals(3, result)
    }

    @Test
    fun `findMaxDisplayOrder - 데이터 없음`() {
        whenever(projectRepository.findMaxDisplayOrder(userId))
            .thenReturn(null)

        val result = projectRepository.findMaxDisplayOrder(userId)

        verify(projectRepository).findMaxDisplayOrder(userId)
        assertEquals(null, result)
    }

    private fun createProject(): Project =
        Project.create(
            userId = userId,
            name = "프로젝트 이름",
        )
}
