package com.didit.application.organization

import com.didit.application.organization.exception.DuplicateProjectNameException
import com.didit.application.organization.exception.ProjectNotFoundException
import com.didit.application.organization.required.ProjectRepository
import com.didit.domain.organization.Project
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
class ProjectModifyServiceTest {
    @Mock
    lateinit var projectRepository: ProjectRepository

    @InjectMocks
    lateinit var projectModifyService: ProjectModifyService

    private val userId = UUID.randomUUID()
    private val projectId = UUID.randomUUID()

    @Test
    fun `프로젝트 이름 수정 성공`() {
        val project = Project.create(userId, "기존 이름")
        val newName = "새 이름"

        whenever(
            projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId),
        ).thenReturn(project)

        whenever(
            projectRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, newName),
        ).thenReturn(false)

        projectModifyService.updateName(userId, projectId, newName)

        assertThat(project.name).isEqualTo(newName)
        verify(projectRepository)
            .existsByUserIdAndNameAndDeletedAtIsNull(userId, newName)
    }

    @Test
    fun `프로젝트 이름 trim 처리`() {
        val project = Project.create(userId, "기존 이름")
        val newName = "  새 이름  "
        val trimmed = "새 이름"

        whenever(
            projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId),
        ).thenReturn(project)

        whenever(
            projectRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, trimmed),
        ).thenReturn(false)

        projectModifyService.updateName(userId, projectId, newName)

        assertThat(project.name).isEqualTo(trimmed)
    }

    @Test
    fun `프로젝트 이름 중복 시 예외 발생`() {
        val project = Project.create(userId, "기존 이름")
        val newName = "중복 이름"

        whenever(
            projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId),
        ).thenReturn(project)

        whenever(
            projectRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, newName),
        ).thenReturn(true)

        assertThrows<DuplicateProjectNameException> {
            projectModifyService.updateName(userId, projectId, newName)
        }
    }

    @Test
    fun `프로젝트가 존재하지 않으면 예외 발생`() {
        whenever(
            projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId),
        ).thenReturn(null)

        assertThrows<ProjectNotFoundException> {
            projectModifyService.updateName(userId, projectId, "새 이름")
        }
    }

    @Test
    fun `이름이 동일하면 아무 동작도 하지 않는다`() {
        val project = Project.create(userId, "같은 이름")

        whenever(
            projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId),
        ).thenReturn(project)

        projectModifyService.updateName(userId, projectId, "같은 이름")

        verify(projectRepository, org.mockito.kotlin.never())
            .existsByUserIdAndNameAndDeletedAtIsNull(any(), any())
    }
}
