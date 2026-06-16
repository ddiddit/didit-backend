package com.didit.domain.prompt

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PromptTest {
    @Test
    fun `update - 내용이 정상적으로 변경된다`() {
        val prompt = Prompt(jobType = PromptJobType.DEVELOPER, promptType = PromptType.DEEP_QUESTION, content = "기존 내용")

        prompt.update(content = "새로운 내용", updatedBy = "admin-id")

        assertThat(prompt.content).isEqualTo("새로운 내용")
        assertThat(prompt.updatedBy).isEqualTo("admin-id")
    }

    @Test
    fun `update - 빈 내용으로 수정하면 예외가 발생한다`() {
        val prompt = Prompt(jobType = PromptJobType.DEVELOPER, promptType = PromptType.DEEP_QUESTION, content = "기존 내용")

        assertThatThrownBy { prompt.update(content = "", updatedBy = "admin-id") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("프롬프트 내용은 비어 있을 수 없습니다.")
    }

    @Test
    fun `update - 공백만 있는 내용으로 수정하면 예외가 발생한다`() {
        val prompt = Prompt(jobType = PromptJobType.DEVELOPER, promptType = PromptType.DEEP_QUESTION, content = "기존 내용")

        assertThatThrownBy { prompt.update(content = "   ", updatedBy = "admin-id") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("프롬프트 내용은 비어 있을 수 없습니다.")
    }

    @Test
    fun `update - updatedAt이 갱신된다`() {
        val prompt = Prompt(jobType = PromptJobType.DEVELOPER, promptType = PromptType.DEEP_QUESTION, content = "기존 내용")
        val before = prompt.updatedAt

        Thread.sleep(10)
        prompt.update(content = "새로운 내용", updatedBy = "admin-id")

        assertThat(prompt.updatedAt).isAfter(before)
    }
}
