// required/AIClientTest.kt
package com.didit.application.retrospect.required

import com.didit.application.retrospect.dto.AISummaryResponse
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
    fun `generateSummaryWithTitle - 제목과 요약을 반환한다`() {
        val answers = listOf("Q1 답변", "Q2 답변", "Q3 답변", "Q4 답변")
        val expected =
            AISummaryResponse(
                title = "오늘의 회고",
                feedback = "피드백",
                insight = "인사이트",
                doneWork = "한 일",
                blockedPoint = "막힌 지점",
                solutionProcess = "해결 과정",
                lessonLearned = "배운 점",
            )
        whenever(aiClient.generateSummaryWithTitle(Job.DEVELOPER, answers)).thenReturn(expected)

        val result = aiClient.generateSummaryWithTitle(Job.DEVELOPER, answers)

        verify(aiClient).generateSummaryWithTitle(Job.DEVELOPER, answers)
        assertThat(result.title).isNotBlank()
        assertThat(result.feedback).isNotBlank()
        assertThat(result.insight).isNotBlank()
    }
}
