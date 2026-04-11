package com.didit.application.retrospect.required

import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.dto.InsightResponse
import com.didit.application.retrospect.dto.NextActionResponse
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
        val expected =
            GeneratedDeepQuestion(
                content = "비슷한 상황이 또 생긴다면 그때는 어떻게 할 것 같나요?",
                inputTokens = 120,
                outputTokens = 25,
            )

        whenever(aiClient.generateDeepQuestion(Job.DEVELOPER, answers))
            .thenReturn(expected)

        val result = aiClient.generateDeepQuestion(Job.DEVELOPER, answers)

        verify(aiClient).generateDeepQuestion(Job.DEVELOPER, answers)
        assertThat(result.content).isNotBlank()
        assertThat(result.inputTokens).isPositive()
        assertThat(result.outputTokens).isPositive()
    }

    @Test
    fun `generateSummaryWithTitle - 제목과 요약을 반환한다`() {
        val answers = listOf("Q1 답변", "Q2 답변", "Q3 답변", "Q4 답변")
        val expected =
            AISummaryResponse(
                title = "오늘의 회고",
                summary = "오늘 회고 요약 문장입니다.",
                blockedPoint = listOf("막힌 지점"),
                solutionProcess = listOf("해결 과정"),
                lessonLearned = listOf("배운 점"),
                insight =
                    InsightResponse(
                        title = "인사이트 제목",
                        description = "인사이트 설명",
                    ),
                nextAction =
                    NextActionResponse(
                        title = "다음 액션 제목",
                        description = "다음 액션 설명",
                    ),
            )

        whenever(aiClient.generateSummaryWithTitle(Job.DEVELOPER, answers))
            .thenReturn(expected)

        val result = aiClient.generateSummaryWithTitle(Job.DEVELOPER, answers)

        verify(aiClient).generateSummaryWithTitle(Job.DEVELOPER, answers)
        assertThat(result.title).isNotBlank()
        assertThat(result.summary).isNotBlank()
        assertThat(result.insight.title).isNotBlank()
        assertThat(result.insight.description).isNotBlank()
        assertThat(result.nextAction.title).isNotBlank()
        assertThat(result.nextAction.description).isNotBlank()
    }
}
