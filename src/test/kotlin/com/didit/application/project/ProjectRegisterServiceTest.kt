package com.didit.application.project

import com.didit.application.auth.provided.UserFinder
import com.didit.application.project.exception.DuplicateProjectNameException
import com.didit.application.project.required.ProjectRepository
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

        whenever(projectRepository.save(any()))
            .thenAnswer { it.arguments[0] }

        val result = projectRegisterService.create(userId, name)

        verify(projectRepository).save(any())

        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.name).isEqualTo(name)
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
}
