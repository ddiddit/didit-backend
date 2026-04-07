package com.didit.application.organization

import com.didit.application.auth.provided.UserFinder
import com.didit.application.organization.exception.DuplicateProjectNameException
import com.didit.application.organization.exception.ProjectNotFoundException
import com.didit.application.organization.required.ProjectRepository
import com.didit.domain.organization.Project
import com.didit.support.UserFixture
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
class ProjectRegisterServiceTest {
    @Mock
    lateinit var projectRepository: ProjectRepository

    @Mock
    lateinit var userFinder: UserFinder

    @InjectMocks
    lateinit var projectRegisterService: ProjectRegisterService

    private val userId = UUID.randomUUID()

    @Test
    fun `프로젝트 생성 성공`() {
        val name = "프로젝트 이름"
        val user = UserFixture.createOnboarded()

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)

        whenever(
            projectRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, name),
        ).thenReturn(false)

        whenever(projectRepository.findMaxDisplayOrder(userId)).thenReturn(null)

        whenever(projectRepository.save(any()))
            .thenAnswer { it.arguments[0] }

        val result = projectRegisterService.create(userId, name)

        verify(projectRepository).save(any())

        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.name).isEqualTo(name)
        assertThat(result.displayOrder).isNull()
    }

    @Test
    fun `프로젝트 생성 성공 - reorder 이후 (맨 뒤 추가)`() {
        val name = "프로젝트 이름"
        val user = UserFixture.createOnboarded()

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)

        whenever(
            projectRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, name),
        ).thenReturn(false)

        whenever(projectRepository.findMaxDisplayOrder(userId)).thenReturn(3)

        whenever(projectRepository.save(any()))
            .thenAnswer { it.arguments[0] }

        val result = projectRegisterService.create(userId, name)

        assertThat(result.displayOrder).isEqualTo(4)
    }

    @Test
    fun `프로젝트 이름 중복 시 예외 발생`() {
        val name = "프로젝트 이름"
        val user = UserFixture.createOnboarded()

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)

        whenever(
            projectRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, name),
        ).thenReturn(true)

        assertThrows<DuplicateProjectNameException> {
            projectRegisterService.create(userId, name)
        }
    }

    @Test
    fun `프로젝트 이름 trim 처리`() {
        val name = "  프로젝트 이름  "
        val trimmed = "프로젝트 이름"
        val user = UserFixture.createOnboarded()

        whenever(userFinder.findByIdOrThrow(userId)).thenReturn(user)

        whenever(
            projectRepository.existsByUserIdAndNameAndDeletedAtIsNull(userId, trimmed),
        ).thenReturn(false)

        whenever(projectRepository.save(any()))
            .thenAnswer { it.arguments[0] }

        val result = projectRegisterService.create(userId, name)

        assertThat(result.name).isEqualTo(trimmed)
    }

    @Test
    fun `프로젝트 순서 변경 성공`() {
        val project1 = Project.create(userId, "A")
        val project2 = Project.create(userId, "B")
        val project3 = Project.create(userId, "C")

        val projects = listOf(project1, project2, project3)

        whenever(projectRepository.findAllByUserIdAndDeletedAtIsNull(userId))
            .thenReturn(projects)

        val requestOrder = listOf(project3.id, project1.id, project2.id)

        projectRegisterService.reorder(userId, requestOrder)

        assertThat(project3.displayOrder).isEqualTo(1)
        assertThat(project1.displayOrder).isEqualTo(2)
        assertThat(project2.displayOrder).isEqualTo(3)
    }

    @Test
    fun `프로젝트 순서 변경 실패 - 개수 불일치`() {
        val project1 = Project.create(userId, "A")

        whenever(projectRepository.findAllByUserIdAndDeletedAtIsNull(userId))
            .thenReturn(listOf(project1))

        val requestOrder = listOf(UUID.randomUUID(), UUID.randomUUID())

        assertThrows<IllegalArgumentException> {
            projectRegisterService.reorder(userId, requestOrder)
        }
    }

    @Test
    fun `프로젝트 순서 변경 실패 - 중복 ID`() {
        val id = UUID.randomUUID()

        val project = Project.create(userId, "A")

        whenever(projectRepository.findAllByUserIdAndDeletedAtIsNull(userId))
            .thenReturn(listOf(project))

        val requestOrder = listOf(id, id)

        assertThrows<IllegalArgumentException> {
            projectRegisterService.reorder(userId, requestOrder)
        }
    }

    @Test
    fun `프로젝트 순서 변경 실패 - 존재하지 않는 프로젝트`() {
        val project = Project.create(userId, "A")

        whenever(projectRepository.findAllByUserIdAndDeletedAtIsNull(userId))
            .thenReturn(listOf(project))

        val requestOrder = listOf(UUID.randomUUID())

        assertThrows<ProjectNotFoundException> {
            projectRegisterService.reorder(userId, requestOrder)
        }
    }
}
