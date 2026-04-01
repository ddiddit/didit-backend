package com.didit.application.retrospect

import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.exception.DailyLimitExceededException
import com.didit.application.retrospect.exception.RetrospectiveAlreadyCompletedException
import com.didit.application.retrospect.exception.RetrospectiveNotInProgressException
import com.didit.application.retrospect.exception.SpeechEmptyFileException
import com.didit.application.retrospect.exception.SpeechEmptyResultException
import com.didit.application.retrospect.exception.SpeechUnsupportedFileException
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.application.retrospect.required.SpeechClient
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveSummary
import com.didit.domain.shared.Job
import com.didit.support.RetrospectiveFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RetrospectServiceTest {
    @Mock lateinit var retrospectiveRepository: RetrospectiveRepository

    @Mock lateinit var retrospectiveFinder: RetrospectiveFinder

    @Mock lateinit var speechClient: SpeechClient

    @Mock lateinit var aiClient: AIClient

    @Mock lateinit var userFinder: UserFinder

    private lateinit var retrospectService: RetrospectService

    private val userId = UUID.randomUUID()
    private val retrospectiveId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        retrospectService =
            RetrospectService(
                retrospectiveRepository = retrospectiveRepository,
                retrospectiveFinder = retrospectiveFinder,
                speechClient = speechClient,
                aiClient = aiClient,
                userFinder = userFinder,
            )
    }

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

    private fun inProgressRetrospective(): Retrospective =
        Retrospective.create(userId).apply {
            addMessage(ChatMessage.question(this, "오늘 어떤 일을 하셨나요?", QuestionType.Q1))
            startProgress()
        }

    @Test
    fun `start - 회고를 시작하고 저장한다`() {
        whenever(retrospectiveFinder.countByUserIdAndDate(any(), any())).thenReturn(0)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.start(userId)

        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.isPending()).isTrue()
        verify(retrospectiveRepository).save(any())
    }

    @Test
    fun `start - 오늘 3회 이상이면 예외가 발생한다`() {
        whenever(retrospectiveFinder.countByUserIdAndDate(any(), any())).thenReturn(3)

        assertThrows<DailyLimitExceededException> {
            retrospectService.start(userId)
        }
        verify(retrospectiveRepository, never()).save(any())
    }

    fun `submitTextAnswer - Q1 답변 시 PENDING에서 IN_PROGRESS로 전환된다`() {
        val retro =
            Retrospective.create(userId).apply {
                addMessage(ChatMessage.question(this, "오늘 어떤 일을 하셨나요?", QuestionType.Q1))
            }
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.submitTextAnswer(retrospectiveId, userId, "Q1 답변")

        assertThat(retro.isInProgress()).isTrue()
        assertThat(result.nextQuestionType).isEqualTo(QuestionType.Q2)
        assertThat(result.isReadyToComplete).isFalse()
    }

    @Test
    fun `submitTextAnswer - Q3 답변 시 심화 질문 생성 메시지를 반환한다`() {
        val retro =
            inProgressRetrospective().apply {
                addMessage(ChatMessage.userAnswer(this, "Q1 답변", QuestionType.Q1, InputType.TEXT))
                addMessage(ChatMessage.question(this, "Q2", QuestionType.Q2))
                addMessage(ChatMessage.userAnswer(this, "Q2 답변", QuestionType.Q2, InputType.TEXT))
                addMessage(ChatMessage.question(this, "Q3", QuestionType.Q3))
            }
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.submitTextAnswer(retrospectiveId, userId, "Q3 답변")

        assertThat(result.nextQuestionType).isEqualTo(QuestionType.Q4_DEEP)
        assertThat(result.isReadyToComplete).isFalse()
    }

    @Test
    fun `submitTextAnswer - 이미 완료된 회고에 답변 시 예외가 발생한다`() {
        val retro = RetrospectiveFixture.createCompleted(userId)
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        assertThrows<RetrospectiveAlreadyCompletedException> {
            retrospectService.submitTextAnswer(retrospectiveId, userId, "답변")
        }
    }

    @Test
    fun `submitVoiceAnswer - wav 파일을 변환해서 답변을 제출한다`() {
        val retro = inProgressRetrospective()
        val audioBytes = ByteArray(100) { 1 }
        val filename = "voice.wav"

        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(speechClient.transcribe(audioBytes, filename)).thenReturn("음성 변환된 텍스트")
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.submitVoiceAnswer(retrospectiveId, userId, audioBytes, filename)

        assertThat(result.nextQuestionType).isEqualTo(QuestionType.Q2)
        verify(speechClient).transcribe(audioBytes, filename)
    }

    @Test
    fun `submitVoiceAnswer - 빈 파일이면 예외가 발생한다`() {
        assertThrows<SpeechEmptyFileException> {
            retrospectService.submitVoiceAnswer(retrospectiveId, userId, ByteArray(0), "voice.wav")
        }
        verify(speechClient, never()).transcribe(any(), any())
    }

    @Test
    fun `submitVoiceAnswer - wav가 아닌 파일이면 예외가 발생한다`() {
        assertThrows<SpeechUnsupportedFileException> {
            retrospectService.submitVoiceAnswer(retrospectiveId, userId, ByteArray(100), "voice.mp3")
        }
        verify(speechClient, never()).transcribe(any(), any())
    }

    @Test
    fun `submitVoiceAnswer - 음성 인식 결과가 비어있으면 예외가 발생한다`() {
        val audioBytes = ByteArray(100) { 1 }
        whenever(speechClient.transcribe(audioBytes, "voice.wav")).thenReturn("   ")

        assertThrows<SpeechEmptyResultException> {
            retrospectService.submitVoiceAnswer(retrospectiveId, userId, audioBytes, "voice.wav")
        }
    }

    @Test
    fun `skipDeepQuestion - 심화 질문을 스킵한다`() {
        val retro = inProgressRetrospective()
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        retrospectService.skipDeepQuestion(retrospectiveId, userId)

        verify(retrospectiveRepository).save(any())
    }

    @Test
    fun `skipDeepQuestion - 진행 중이 아닌 회고에 스킵 시 예외가 발생한다`() {
        val retro = Retrospective.create(userId) // PENDING
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        assertThrows<RetrospectiveNotInProgressException> {
            retrospectService.skipDeepQuestion(retrospectiveId, userId)
        }
    }

    @Test
    fun `complete - AI 요약을 생성하고 저장한다`() {
        val retro = inProgressRetrospective()
        val summary = aiSummaryResponse()
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(userFinder.getJobByUserId(userId)).thenReturn(Job.DEVELOPER)
        whenever(aiClient.generateSummaryWithTitle(any(), any())).thenReturn(summary)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.complete(retrospectiveId, userId)

        assertThat(result.title).isEqualTo(summary.title)
        assertThat(retro.summary).isNotNull()
        verify(retrospectiveRepository).save(retro)
    }

    @Test
    fun `complete - 이미 완료된 회고면 예외가 발생한다`() {
        val retro = RetrospectiveFixture.createCompleted(userId)
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        assertThrows<RetrospectiveAlreadyCompletedException> {
            retrospectService.complete(retrospectiveId, userId)
        }
    }

    @Test
    fun `save - 제목으로 회고를 완료 처리한다`() {
        val retro =
            inProgressRetrospective().apply {
                saveSummary(
                    RetrospectiveSummary(
                        feedback = "피드백",
                        insight = "인사이트",
                        doneWork = "한 일",
                        blockedPoint = "막힌 지점",
                        solutionProcess = "해결 과정",
                        lessonLearned = "배운 점",
                    ),
                )
            }
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.save(retrospectiveId, userId, "오늘의 회고")

        assertThat(result.isCompleted()).isTrue()
        assertThat(result.title).isEqualTo("오늘의 회고")
    }

    @Test
    fun `save - 이미 완료된 회고면 예외가 발생한다`() {
        val retro = RetrospectiveFixture.createCompleted(userId)
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        assertThrows<RetrospectiveAlreadyCompletedException> {
            retrospectService.save(retrospectiveId, userId, "제목")
        }
    }

    @Test
    fun `restart - 기존 회고를 삭제하고 새로 시작한다`() {
        val retro = inProgressRetrospective()
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(retrospectiveFinder.countByUserIdAndDate(any(), any())).thenReturn(0)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.restart(retrospectiveId, userId)

        assertThat(retro.isDeleted()).isTrue()
        assertThat(result.isPending()).isTrue()
    }

    @Test
    fun `updateTitle - 제목을 수정한다`() {
        val retro = RetrospectiveFixture.createCompleted(userId)
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        retrospectService.updateTitle(retrospectiveId, userId, "새로운 제목")

        assertThat(retro.title).isEqualTo("새로운 제목")
    }

    @Test
    fun `delete - 회고를 소프트 삭제한다`() {
        val retro = inProgressRetrospective()
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        retrospectService.delete(retrospectiveId, userId)

        assertThat(retro.isDeleted()).isTrue()
    }

    @Test
    fun `exit - PENDING 상태면 소프트 삭제한다`() {
        val retro = Retrospective.create(userId)
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        retrospectService.exit(retrospectiveId, userId)

        assertThat(retro.isDeleted()).isTrue()
    }

    @Test
    fun `exit - IN_PROGRESS 상태면 삭제하지 않는다`() {
        val retro = inProgressRetrospective()
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        retrospectService.exit(retrospectiveId, userId)

        assertThat(retro.isDeleted()).isFalse()
        verify(retrospectiveRepository, never()).save(any())
    }
}
