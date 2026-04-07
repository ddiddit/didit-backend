package com.didit.application.organization

import com.didit.application.organization.exception.DuplicateProjectNameException
import com.didit.application.organization.exception.ProjectNotFoundException
import com.didit.application.organization.required.ProjectRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.organization.Project
import com.didit.domain.retrospect.Retrospective
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ProjectModifierServiceTest {
    @Mock
    lateinit var projectRepository: ProjectRepository

    @Mock
    lateinit var retrospectiveRepository: RetrospectiveRepository

    @InjectMocks
    lateinit var projectModifierService: ProjectModifierService

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

        projectModifierService.updateName(userId, projectId, newName)

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

        projectModifierService.updateName(userId, projectId, newName)

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
            projectModifierService.updateName(userId, projectId, newName)
        }
    }

    @Test
    fun `이름이 동일하면 아무 동작도 하지 않는다`() {
        val project = Project.create(userId, "같은 이름")

        whenever(
            projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId),
        ).thenReturn(project)

        projectModifierService.updateName(userId, projectId, "같은 이름")

        verify(projectRepository, org.mockito.kotlin.never())
            .existsByUserIdAndNameAndDeletedAtIsNull(any(), any())
    }

    @Test
    fun `프로젝트 삭제 시 회고에서 detach 되고 soft delete 된다`() {
        val project =
            Project(
                id = projectId,
                userId = userId,
                name = "프로젝트",
            )

        val retrospectives =
            listOf(
                Retrospective.create(userId).apply { registerProject(project.id) },
                Retrospective.create(userId).apply { registerProject(project.id) },
            )

        whenever(
            projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId),
        ).thenReturn(project)

        whenever(
            retrospectiveRepository.findAllByProjectIdAndDeletedAtIsNull(projectId),
        ).thenReturn(retrospectives)

        projectModifierService.deleteProject(userId, projectId)

        retrospectives.forEach {
            assertThat(it.projectId).isNull()
        }

        assertThat(project.isDeleted()).isTrue()

        verify(projectRepository)
            .findByIdAndUserIdAndDeletedAtIsNull(projectId, userId)

        verify(retrospectiveRepository)
            .findAllByProjectIdAndDeletedAtIsNull(projectId)
    }

    @Test
    fun `프로젝트가 존재하지 않으면 예외 발생`() {
        whenever(
            projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId),
        ).thenReturn(null)

        assertThrows<ProjectNotFoundException> {
            projectModifierService.deleteProject(userId, projectId)
        }
    }

    @Test
    fun `회고가 없는 경우에도 프로젝트는 삭제된다`() {
        val project = Project.create(userId, "프로젝트")

        whenever(
            projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId),
        ).thenReturn(project)

        whenever(
            retrospectiveRepository.findAllByProjectIdAndDeletedAtIsNull(projectId),
        ).thenReturn(emptyList())

        projectModifierService.deleteProject(userId, projectId)
        assertThat(project.isDeleted()).isTrue()
    }
}
