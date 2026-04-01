package com.didit.application.retrospect.provided

import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RetrospectiveRegisterTest {
    @Mock
    lateinit var retrospectiveRegister: RetrospectiveRegister

    private val userId = UUID.randomUUID()
    private val retrospectiveId = UUID.randomUUID()

    private fun aiSummaryResponse() =
        AISummaryResponse(
            title = "오늘의 회고",
            feedback = "피드백",
            insight = "인사이트",
            doneWork = "한 일",
            blockedPoint = "막힌 지점",
            solutionProcess = "해결 과정",
            lessonLearned = "배운 점",
        )

    @Test
    fun `start - 회고를 시작한다`() {
        val retro = Retrospective.create(userId)
        whenever(retrospectiveRegister.start(userId)).thenReturn(retro)

        val result = retrospectiveRegister.start(userId)

        verify(retrospectiveRegister).start(userId)
        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `submitTextAnswer - 텍스트 답변을 제출하고 다음 질문을 반환한다`() {
        val response =
            SubmitAnswerResponse(
                nextQuestionType = QuestionType.Q2,
                nextQuestionContent = "진행하면서 어떤 시도, 혹은 어려움이 있었나요?",
                isReadyToComplete = false,
            )
        whenever(retrospectiveRegister.submitTextAnswer(retrospectiveId, userId, "열심히 했습니다."))
            .thenReturn(response)

        val result = retrospectiveRegister.submitTextAnswer(retrospectiveId, userId, "열심히 했습니다.")

        verify(retrospectiveRegister).submitTextAnswer(retrospectiveId, userId, "열심히 했습니다.")
        assertThat(result.nextQuestionType).isEqualTo(QuestionType.Q2)
        assertThat(result.isReadyToComplete).isFalse()
    }

    @Test
    fun `submitTextAnswer - Q4 답변 후 완료 준비 상태를 반환한다`() {
        val response =
            SubmitAnswerResponse(
                nextQuestionType = null,
                nextQuestionContent = null,
                isReadyToComplete = true,
            )
        whenever(retrospectiveRegister.submitTextAnswer(retrospectiveId, userId, "Q4 답변"))
            .thenReturn(response)

        val result = retrospectiveRegister.submitTextAnswer(retrospectiveId, userId, "Q4 답변")

        verify(retrospectiveRegister).submitTextAnswer(retrospectiveId, userId, "Q4 답변")
        assertThat(result.isReadyToComplete).isTrue()
        assertThat(result.nextQuestionType).isNull()
    }

    @Test
    fun `submitVoiceAnswer - 음성 답변을 제출하고 다음 질문을 반환한다`() {
        val audioBytes = ByteArray(100)
        val filename = "voice.wav"
        val response =
            SubmitAnswerResponse(
                nextQuestionType = QuestionType.Q2,
                nextQuestionContent = "진행하면서 어떤 시도, 혹은 어려움이 있었나요?",
                isReadyToComplete = false,
            )
        whenever(retrospectiveRegister.submitVoiceAnswer(retrospectiveId, userId, audioBytes, filename))
            .thenReturn(response)

        val result = retrospectiveRegister.submitVoiceAnswer(retrospectiveId, userId, audioBytes, filename)

        verify(retrospectiveRegister).submitVoiceAnswer(retrospectiveId, userId, audioBytes, filename)
        assertThat(result.nextQuestionType).isEqualTo(QuestionType.Q2)
        assertThat(result.isReadyToComplete).isFalse()
    }

    @Test
    fun `skipDeepQuestion - 심화 질문을 스킵한다`() {
        retrospectiveRegister.skipDeepQuestion(retrospectiveId, userId)

        verify(retrospectiveRegister).skipDeepQuestion(retrospectiveId, userId)
    }

    @Test
    fun `complete - AI 요약과 제목을 반환한다`() {
        val summary = aiSummaryResponse()
        whenever(retrospectiveRegister.complete(retrospectiveId, userId)).thenReturn(summary)

        val result = retrospectiveRegister.complete(retrospectiveId, userId)

        verify(retrospectiveRegister).complete(retrospectiveId, userId)
        assertThat(result.title).isNotBlank()
        assertThat(result.feedback).isNotBlank()
    }

    @Test
    fun `save - 제목으로 회고를 저장한다`() {
        val retro = Retrospective.create(userId)
        whenever(retrospectiveRegister.save(retrospectiveId, userId, "오늘의 회고"))
            .thenReturn(retro)

        val result = retrospectiveRegister.save(retrospectiveId, userId, "오늘의 회고")

        verify(retrospectiveRegister).save(retrospectiveId, userId, "오늘의 회고")
        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `restart - 회고를 다시 시작한다`() {
        val retro = Retrospective.create(userId)
        whenever(retrospectiveRegister.restart(retrospectiveId, userId)).thenReturn(retro)

        val result = retrospectiveRegister.restart(retrospectiveId, userId)

        verify(retrospectiveRegister).restart(retrospectiveId, userId)
        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `updateTitle - 제목을 수정한다`() {
        retrospectiveRegister.updateTitle(retrospectiveId, userId, "수정된 제목")

        verify(retrospectiveRegister).updateTitle(retrospectiveId, userId, "수정된 제목")
    }

    @Test
    fun `delete - 회고를 삭제한다`() {
        retrospectiveRegister.delete(retrospectiveId, userId)

        verify(retrospectiveRegister).delete(retrospectiveId, userId)
    }

    @Test
    fun `exit - 나가기를 처리한다`() {
        retrospectiveRegister.exit(retrospectiveId, userId)

        verify(retrospectiveRegister).exit(retrospectiveId, userId)
    }
}
