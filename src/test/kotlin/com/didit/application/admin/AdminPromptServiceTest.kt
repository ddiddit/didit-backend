package com.didit.application.admin

import com.didit.application.common.exception.BusinessException
import com.didit.application.prompt.required.PromptRepository
import com.didit.domain.prompt.Prompt
import com.didit.domain.prompt.PromptJobType
import com.didit.domain.prompt.PromptType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminPromptServiceTest {
    @Mock
    lateinit var promptRepository: PromptRepository

    @InjectMocks
    lateinit var adminPromptService: AdminPromptService

    private fun createPrompt(
        jobType: PromptJobType = PromptJobType.DEVELOPER,
        promptType: PromptType = PromptType.DEEP_QUESTION,
        content: String = "기존 프롬프트 내용",
    ) = Prompt(jobType = jobType, promptType = promptType, content = content)

    @Test
    fun `프롬프트 전체 조회`() {
        val prompts =
            listOf(
                createPrompt(jobType = PromptJobType.DEVELOPER),
                createPrompt(jobType = PromptJobType.PLANNER),
            )
        whenever(promptRepository.findAll()).thenReturn(prompts)

        val result = adminPromptService.findAll()

        assertThat(result).hasSize(2)
        assertThat(result[0].jobType).isEqualTo("DEVELOPER")
        assertThat(result[1].jobType).isEqualTo("PLANNER")
    }

    @Test
    fun `프롬프트 단건 조회 성공`() {
        val id = UUID.randomUUID()
        val prompt = createPrompt(content = "심화 질문 프롬프트")
        whenever(promptRepository.findById(id)).thenReturn(prompt)

        val result = adminPromptService.findById(id)

        assertThat(result.content).isEqualTo("심화 질문 프롬프트")
    }

    @Test
    fun `프롬프트 단건 조회 실패 - 존재하지 않는 ID`() {
        val id = UUID.randomUUID()
        whenever(promptRepository.findById(id)).thenReturn(null)

        assertThatThrownBy { adminPromptService.findById(id) }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `프롬프트 수정 성공`() {
        val id = UUID.randomUUID()
        val prompt = createPrompt()
        whenever(promptRepository.findById(id)).thenReturn(prompt)
        whenever(promptRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = adminPromptService.update(id, "수정된 내용", "admin-id")

        assertThat(result.content).isEqualTo("수정된 내용")
        assertThat(result.updatedBy).isEqualTo("admin-id")
    }

    @Test
    fun `프롬프트 수정 실패 - 존재하지 않는 ID`() {
        val id = UUID.randomUUID()
        whenever(promptRepository.findById(id)).thenReturn(null)

        assertThatThrownBy { adminPromptService.update(id, "수정된 내용", "admin-id") }
            .isInstanceOf(BusinessException::class.java)
    }
}
