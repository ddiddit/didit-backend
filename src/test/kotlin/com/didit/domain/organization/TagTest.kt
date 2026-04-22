package com.didit.domain.organization

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class TagTest {
    private val userId = UUID.randomUUID()

    @Test
    fun `태그 생성 성공`() {
        val name = "테스트 태그"

        val tag = Tag.create(userId, name)

        assertThat(tag.userId).isEqualTo(userId)
        assertThat(tag.name).isEqualTo(name)
        assertThat(tag.deletedAt).isNull()
    }

    @Test
    fun `태그 이름 trim 처리`() {
        val name = "  테스트 태그  "

        val tag = Tag.create(userId, name)

        assertThat(tag.name).isEqualTo("테스트 태그")
    }

    @Test
    fun `태그 이름이 공백이면 예외 발생`() {
        val name = "   "

        val exception =
            assertThrows<IllegalArgumentException> {
                Tag.create(userId, name)
            }

        assertThat(exception.message).isEqualTo("태그명은 비어있을 수 없습니다.")
    }

    @Test
    fun `delete 호출 시 deletedAt 설정`() {
        val tag = Tag.create(userId, "테스트 태그")
        assertThat(tag.deletedAt).isNull()

        tag.delete()

        assertThat(tag.deletedAt).isNotNull
        assertThat(tag.deletedAt).isBeforeOrEqualTo(LocalDateTime.now())
    }
}
