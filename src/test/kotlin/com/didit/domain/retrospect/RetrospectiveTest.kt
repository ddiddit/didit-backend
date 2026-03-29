package com.didit.domain.retrospect

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RetrospectiveTest {
    private val userId = UUID.randomUUID()

    private fun retrospective() = Retrospective.create(userId)

    private fun summary() =
        RetrospectiveSummary(
            feedback = "피드백",
            insight = "인사이트",
            doneWork = "한 일",
            blockedPoint = "막힌 지점",
            solutionProcess = "해결 과정",
            lessonLearned = "배운 점",
        )

    @Test
    fun `create - 초기 상태는 PENDING이고 summary는 null이다`() {
        val retro = retrospective()

        assertEquals(RetroStatus.PENDING, retro.status)
        assertNull(retro.summary)
        assertNull(retro.title)
        assertTrue(retro.isPending())
        assertFalse(retro.isInProgress())
        assertFalse(retro.isCompleted())
    }

    @Test
    fun `startProgress - 상태가 IN_PROGRESS로 변경된다`() {
        val retro = retrospective()

        retro.startProgress()

        assertTrue(retro.isInProgress())
        assertFalse(retro.isPending())
        assertFalse(retro.isCompleted())
    }

    @Test
    fun `complete - 정상 완료 시 status가 COMPLETED로 변경된다`() {
        val retro = retrospective().apply { startProgress() }

        retro.complete(
            title = "오늘의 회고",
            summary = summary(),
            inputTokens = 100,
            outputTokens = 200,
        )

        assertTrue(retro.isCompleted())
        assertFalse(retro.isInProgress())
        assertEquals("오늘의 회고", retro.title)
        assertEquals(100, retro.inputTokens)
        assertEquals(200, retro.outputTokens)
    }

    @Test
    fun `complete - 제목이 빈 값이면 예외가 발생한다`() {
        val retro = retrospective().apply { startProgress() }

        assertThrows<IllegalArgumentException> {
            retro.complete(
                title = "",
                summary = summary(),
                inputTokens = 100,
                outputTokens = 200,
            )
        }
    }

    @Test
    fun `softDelete - deletedAt이 설정되고 isDeleted가 true가 된다`() {
        val retro = retrospective()

        retro.softDelete()

        assertTrue(retro.isDeleted())
    }

    @Test
    fun `getAnswersUpToQ3 - Q4_DEEP 답변과 스킵된 답변은 제외된다`() {
        val retro = retrospective()
        retro.addMessage(ChatMessage.userAnswer(retro, "Q1 답변", QuestionType.Q1))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q2 답변", QuestionType.Q2))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q3 답변", QuestionType.Q3))
        retro.addMessage(ChatMessage.skippedAnswer(retro, QuestionType.Q4_DEEP))

        val answers = retro.getAnswersUpToQ3()

        assertEquals(3, answers.size)
        assertEquals(listOf("Q1 답변", "Q2 답변", "Q3 답변"), answers)
    }

    @Test
    fun `hasDeepQuestion - AI가 Q4_DEEP 질문을 보낸 경우 true를 반환한다`() {
        val retro = retrospective()
        retro.addMessage(ChatMessage.question(retro, "심화 질문", QuestionType.Q4_DEEP))

        assertTrue(retro.hasDeepQuestion())
    }

    @Test
    fun `currentQuestionType - AI가 보낸 마지막 질문 타입을 반환한다`() {
        val retro = retrospective()
        retro.addMessage(ChatMessage.question(retro, "Q1 질문", QuestionType.Q1))
        retro.addMessage(ChatMessage.question(retro, "Q2 질문", QuestionType.Q2))

        assertEquals(QuestionType.Q2, retro.currentQuestionType())
    }

    @Test
    fun `canAddDeepQuestion - Q1~Q3 답변이 모두 있고 심화 질문이 없으면 true를 반환한다`() {
        val retro = retrospective()
        retro.addMessage(ChatMessage.userAnswer(retro, "Q1 답변", QuestionType.Q1))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q2 답변", QuestionType.Q2))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q3 답변", QuestionType.Q3))

        assertTrue(retro.canAddDeepQuestion())
    }

    @Test
    fun `canAddDeepQuestion - Q1~Q3 답변이 있어도 심화 질문이 이미 있으면 false를 반환한다`() {
        val retro = retrospective()
        retro.addMessage(ChatMessage.userAnswer(retro, "Q1 답변", QuestionType.Q1))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q2 답변", QuestionType.Q2))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q3 답변", QuestionType.Q3))
        retro.addMessage(ChatMessage.question(retro, "심화 질문", QuestionType.Q4_DEEP))

        assertFalse(retro.canAddDeepQuestion())
    }

    @Test
    fun `canAddDeepQuestion - Q1~Q3 답변이 모두 없으면 false를 반환한다`() {
        val retro = retrospective()
        retro.addMessage(ChatMessage.userAnswer(retro, "Q1 답변", QuestionType.Q1))

        assertFalse(retro.canAddDeepQuestion())
    }

    @Test
    fun `updateTitle - 제목이 정상적으로 변경된다`() {
        val retro = retrospective()

        retro.updateTitle("새로운 제목")

        assertThat(retro.title).isEqualTo("새로운 제목")
    }

    @Test
    fun `updateTitle - 빈 제목이면 예외가 발생한다`() {
        val retro = retrospective()

        assertThrows<IllegalArgumentException> {
            retro.updateTitle("")
        }
    }
}
