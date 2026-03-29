package com.didit.application.retrospect.required

import com.didit.domain.shared.Job
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AIClientTest {
    @Mock
    lateinit var aiClient: AIClient

    @Test
    fun `generateDeepQuestion - 심화 질문을 반환한다`() {
        val answers = listOf("Q1 답변", "Q2 답변", "Q3 답변")
        whenever(aiClient.generateDeepQuestion(Job.DEVELOPER, answers))
            .thenReturn("비슷한 상황이 또 생긴다면 그때는 어떻게 할 것 같나요?")

        val result = aiClient.generateDeepQuestion(Job.DEVELOPER, answers)

        verify(aiClient).generateDeepQuestion(Job.DEVELOPER, answers)
        assertThat(result).isNotBlank()
    }

    @Test
    fun `generateSummary - 요약을 반환한다`() {
        val answers = listOf("Q1 답변", "Q2 답변", "Q3 답변", "Q4 답변")
        whenever(aiClient.generateSummary(Job.DEVELOPER, answers))
            .thenReturn("{\"aiFeedback\":\"피드백\",\"insight\":\"인사이트\"}")

        val result = aiClient.generateSummary(Job.DEVELOPER, answers)

        verify(aiClient).generateSummary(Job.DEVELOPER, answers)
        assertThat(result).isNotBlank()
    }
}
