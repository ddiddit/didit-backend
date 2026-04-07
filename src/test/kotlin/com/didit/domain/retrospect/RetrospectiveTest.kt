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
            summary = "오늘 회고 요약 문장입니다.",
            feedback = "피드백",
            insight = "인사이트",
            doneWork = "한 일",
            blockedPoint = "막힌 지점",
            solutionProcess = "해결 과정",
            lessonLearned = "배운 점",
            nextAction = "다음 액션",
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

        retro.saveSummary(summary())
        retro.complete(title = "오늘의 회고")

        assertTrue(retro.isCompleted())
        assertFalse(retro.isInProgress())
        assertEquals("오늘의 회고", retro.title)
        assertThat(retro.completedAt).isNotNull()
    }

    @Test
    fun `complete - 제목이 25자를 초과하면 예외가 발생한다`() {
        val retro = retrospective().apply { startProgress() }

        assertThrows<IllegalArgumentException> {
            retro.complete(title = "열여섯글자제목입니다테스트합니다열여섯글자제목입니다테스트합니다")
        }
    }

    @Test
    fun `updateTitle - 제목이 25자를 초과하면 예외가 발생한다`() {
        val retro = retrospective()

        assertThrows<IllegalArgumentException> {
            retro.updateTitle("열여섯글자제목입니다테스트합니다열여섯글자제목입니다테스트합니다")
        }
    }

    @Test
    fun `complete - 제목이 빈 값이면 예외가 발생한다`() {
        val retro = retrospective().apply { startProgress() }

        assertThrows<IllegalArgumentException> {
            retro.complete(title = "")
        }
    }

    @Test
    fun `saveSummary - summary가 저장된다`() {
        val retro = retrospective()

        retro.saveSummary(summary())

        assertThat(retro.summary).isNotNull()
        assertThat(retro.summary!!.summary).isEqualTo("오늘 회고 요약 문장입니다.")
    }

    @Test
    fun `addTokens - 토큰이 누적된다`() {
        val retro = retrospective()

        retro.addTokens(100, 50)
        retro.addTokens(200, 80)

        assertThat(retro.inputTokens).isEqualTo(300)
        assertThat(retro.outputTokens).isEqualTo(130)
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
        retro.addMessage(ChatMessage.userAnswer(retro, "Q1 답변", QuestionType.Q1, InputType.TEXT))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q2 답변", QuestionType.Q2, InputType.TEXT))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q3 답변", QuestionType.Q3, InputType.TEXT))
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
        retro.addMessage(ChatMessage.userAnswer(retro, "Q1 답변", QuestionType.Q1, InputType.TEXT))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q2 답변", QuestionType.Q2, InputType.TEXT))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q3 답변", QuestionType.Q3, InputType.TEXT))

        assertTrue(retro.canAddDeepQuestion())
    }

    @Test
    fun `canAddDeepQuestion - Q1~Q3 답변이 있어도 심화 질문이 이미 있으면 false를 반환한다`() {
        val retro = retrospective()
        retro.addMessage(ChatMessage.userAnswer(retro, "Q1 답변", QuestionType.Q1, InputType.TEXT))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q2 답변", QuestionType.Q2, InputType.TEXT))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q3 답변", QuestionType.Q3, InputType.TEXT))
        retro.addMessage(ChatMessage.question(retro, "심화 질문", QuestionType.Q4_DEEP))

        assertFalse(retro.canAddDeepQuestion())
    }

    @Test
    fun `canAddDeepQuestion - Q1~Q3 답변이 모두 없으면 false를 반환한다`() {
        val retro = retrospective()
        retro.addMessage(ChatMessage.userAnswer(retro, "Q1 답변", QuestionType.Q1, InputType.TEXT))

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

    @Test
    fun `countDeepQuestionAnswers - 심화질문 답변 수를 반환한다`() {
        val retro = retrospective()
        retro.addMessage(ChatMessage.userAnswer(retro, "Q4 답변1", QuestionType.Q4_DEEP, InputType.TEXT))
        retro.addMessage(ChatMessage.userAnswer(retro, "Q4 답변2", QuestionType.Q4_DEEP, InputType.TEXT))

        assertThat(retro.countDeepQuestionAnswers()).isEqualTo(2)
    }

    @Test
    fun `countDeepQuestionAnswers - 스킵된 답변은 카운트에서 제외된다`() {
        val retro = retrospective()
        retro.addMessage(ChatMessage.userAnswer(retro, "Q4 답변", QuestionType.Q4_DEEP, InputType.TEXT))
        retro.addMessage(ChatMessage.skippedAnswer(retro, QuestionType.Q4_DEEP))

        assertThat(retro.countDeepQuestionAnswers()).isEqualTo(1)
    }

    @Test
    fun `countDeepQuestionAnswers - 심화질문 답변이 없으면 0을 반환한다`() {
        val retro = retrospective()
        retro.addMessage(ChatMessage.userAnswer(retro, "Q1 답변", QuestionType.Q1, InputType.TEXT))

        assertThat(retro.countDeepQuestionAnswers()).isEqualTo(0)
    }

    @Test
    fun `assignProject - projectId가 정상적으로 할당된다`() {
        val retro = retrospective()
        val projectId = UUID.randomUUID()

        retro.assignProject(projectId)

        assertThat(retro.projectId).isEqualTo(projectId)
    }

    @Test
    fun `detachProject - 프로젝트가 정상적으로 제거된다`() {
        val retro = retrospective()
        val projectId = UUID.randomUUID()
        retro.assignProject(projectId)

        retro.detachProject()

        assertThat(retro.projectId).isNull()
    }
}
