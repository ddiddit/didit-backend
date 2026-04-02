package com.didit.domain.organization

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.Test

class ProjectTest {
    private val userId = UUID.randomUUID()

    @Test
    fun `프로젝트 생성 성공`() {
        val name = "프로젝트"

        val project = Project.create(userId, name)

        assertThat(project.userId).isEqualTo(userId)
        assertThat(project.name).isEqualTo(name)
        assertThat(project.deletedAt).isNull()
    }

    @Test
    fun `프로젝트 이름 trim 처리`() {
        val name = "  프로젝트  "

        val project = Project.create(userId, name)

        assertThat(project.name).isEqualTo("프로젝트")
    }

    @Test
    fun `프로젝트 이름이 공백이면 예외 발생`() {
        val name = "   "

        val exception =
            assertThrows<IllegalArgumentException> {
                Project.create(userId, name)
            }

        assertThat(exception.message).isEqualTo("프로젝트 이름은 비어있을 수 없습니다.")
    }

    @Test
    fun `프로젝트 이름이 15자 초과면 예외 발생`() {
        val name = "1234567890123456" // 16자

        val exception =
            assertThrows<IllegalArgumentException> {
                Project.create(userId, name)
            }

        assertThat(exception.message).isEqualTo("프로젝트 이름은 15자를 초과할 수 없습니다.")
    }
}
