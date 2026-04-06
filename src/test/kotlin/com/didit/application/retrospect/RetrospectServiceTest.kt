package com.didit.application.retrospect

import com.didit.application.audit.AuditLogger
import com.didit.application.auth.provided.UserFinder
import com.didit.application.organization.required.ProjectRepository
import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.exception.DailyLimitExceededException
import com.didit.application.retrospect.exception.RetrospectiveAlreadyCompletedException
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.exception.RetrospectiveNotInProgressException
import com.didit.application.retrospect.exception.SpeechEmptyFileException
import com.didit.application.retrospect.exception.SpeechEmptyResultException
import com.didit.application.retrospect.exception.SpeechUnsupportedFileException
import com.didit.application.retrospect.exception.SummaryNotGeneratedException
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.GeneratedDeepQuestion
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.application.retrospect.required.SpeechClient
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveCompletedEvent
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RetrospectServiceTest {
    @Mock
    lateinit var retrospectiveRepository: RetrospectiveRepository

    @Mock
    lateinit var retrospectiveFinder: RetrospectiveFinder

    @Mock
    lateinit var speechClient: SpeechClient

    @Mock
    lateinit var aiClient: AIClient

    @Mock
    lateinit var userFinder: UserFinder

    @Mock
    lateinit var eventPublisher: ApplicationEventPublisher

    @Mock
    lateinit var auditLogger: AuditLogger

    @Mock
    lateinit var projectRepository: ProjectRepository

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
                eventPublisher = eventPublisher,
                auditLogger = auditLogger,
                projectRepository = projectRepository,
            )
    }

    private fun aiSummaryResponse() =
        AISummaryResponse(
            title = "오늘의 회고",
            summary = "오늘 회고 요약 문장입니다.",
            feedback = "피드백",
            insight = "인사이트",
            doneWork = "한 일",
            blockedPoint = listOf("막힌 지점"),
            solutionProcess = listOf("해결 과정"),
            lessonLearned = listOf("배운 점"),
            nextAction = listOf("다음 액션"),
            inputTokens = 100,
            outputTokens = 50,
        )

    private fun inProgressRetrospective(): Retrospective =
        Retrospective.create(userId).apply {
            addMessage(ChatMessage.question(this, "오늘 어떤 일을 하셨나요?", QuestionType.Q1))
            startProgress()
        }

    private fun summaryFixture() =
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

    @Test
    fun `submitAnswer - Q1 텍스트 답변 시 PENDING에서 IN_PROGRESS로 전환된다`() {
        val retro =
            Retrospective.create(userId).apply {
                addMessage(ChatMessage.question(this, "오늘 어떤 일을 하셨나요?", QuestionType.Q1))
            }
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.submitAnswer(retrospectiveId, userId, "Q1 답변")

        assertThat(retro.isInProgress()).isTrue()
        assertThat(result.nextQuestionType).isEqualTo(QuestionType.Q2)
        assertThat(result.isReadyToComplete).isFalse()
        assertThat(result.content).isNull()
    }

    @Test
    fun `submitAnswer - Q3 답변 시 심화 질문 생성 메시지를 반환한다`() {
        val retro =
            inProgressRetrospective().apply {
                addMessage(ChatMessage.userAnswer(this, "Q1 답변", QuestionType.Q1, InputType.TEXT))
                addMessage(ChatMessage.question(this, "Q2", QuestionType.Q2))
                addMessage(ChatMessage.userAnswer(this, "Q2 답변", QuestionType.Q2, InputType.TEXT))
                addMessage(ChatMessage.question(this, "Q3", QuestionType.Q3))
            }

        whenever(retrospectiveFinder.findById(any(), any())).thenReturn(retro)
        whenever(userFinder.getJobByUserId(any())).thenReturn(Job.DEVELOPER)
        whenever(aiClient.generateDeepQuestion(any(), any())).thenReturn(
            GeneratedDeepQuestion(content = "심화 질문입니다.", inputTokens = 50, outputTokens = 20),
        )
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.submitAnswer(retrospectiveId, userId, "Q3 답변")

        assertThat(result.nextQuestionType).isEqualTo(QuestionType.Q4_DEEP)
        assertThat(result.isReadyToComplete).isFalse()
    }

    @Test
    fun `submitAnswer - 이미 완료된 회고에 답변 시 예외가 발생한다`() {
        val retro = RetrospectiveFixture.createCompleted(userId)
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        assertThrows<RetrospectiveAlreadyCompletedException> {
            retrospectService.submitAnswer(retrospectiveId, userId, "답변")
        }
    }

    @Test
    fun `submitVoiceAnswer - m4a 파일을 변환해서 답변을 제출하고 텍스트를 반환한다`() {
        val retro = inProgressRetrospective()
        val audioBytes = ByteArray(100) { 1 }
        val filename = "voice.m4a"

        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(speechClient.transcribe(audioBytes, filename)).thenReturn("음성 변환된 텍스트")
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.submitVoiceAnswer(retrospectiveId, userId, audioBytes, filename)

        assertThat(result.content).isEqualTo("음성 변환된 텍스트")
        assertThat(result.nextQuestionType).isEqualTo(QuestionType.Q2)
        verify(speechClient).transcribe(audioBytes, filename)
    }

    @Test
    fun `submitVoiceAnswer - mp3 파일을 변환해서 답변을 제출하고 텍스트를 반환한다`() {
        val retro = inProgressRetrospective()
        val audioBytes = ByteArray(100) { 1 }
        val filename = "voice.mp3"

        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(speechClient.transcribe(audioBytes, filename)).thenReturn("음성 변환된 텍스트")
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.submitVoiceAnswer(retrospectiveId, userId, audioBytes, filename)

        assertThat(result.content).isEqualTo("음성 변환된 텍스트")
        assertThat(result.nextQuestionType).isEqualTo(QuestionType.Q2)
        verify(speechClient).transcribe(audioBytes, filename)
    }

    @Test
    fun `submitVoiceAnswer - 회고가 완료된 상태면 STT 호출 없이 예외가 발생한다`() {
        val retro = RetrospectiveFixture.createCompleted(userId)
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        assertThrows<RetrospectiveAlreadyCompletedException> {
            retrospectService.submitVoiceAnswer(retrospectiveId, userId, ByteArray(100) { 1 }, "voice.wav")
        }
        verify(speechClient, never()).transcribe(any(), any())
    }

    @Test
    fun `submitVoiceAnswer - 빈 파일이면 STT 호출 없이 예외가 발생한다`() {
        val retro = inProgressRetrospective()
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        assertThrows<SpeechEmptyFileException> {
            retrospectService.submitVoiceAnswer(retrospectiveId, userId, ByteArray(0), "voice.wav")
        }
        verify(speechClient, never()).transcribe(any(), any())
    }

    @Test
    fun `submitVoiceAnswer - 지원하지 않는 파일 형식이면 STT 호출 없이 예외가 발생한다`() {
        val retro = inProgressRetrospective()
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        assertThrows<SpeechUnsupportedFileException> {
            retrospectService.submitVoiceAnswer(retrospectiveId, userId, ByteArray(100), "voice.txt")
        }
        verify(speechClient, never()).transcribe(any(), any())
    }

    @Test
    fun `submitVoiceAnswer - 음성 인식 결과가 비어있으면 예외가 발생한다`() {
        val retro = inProgressRetrospective()
        val audioBytes = ByteArray(100) { 1 }
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
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
        val retro = Retrospective.create(userId)
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        assertThrows<RetrospectiveNotInProgressException> {
            retrospectService.skipDeepQuestion(retrospectiveId, userId)
        }
    }

    @Test
    fun `complete - AI 요약을 생성하고 토큰을 포함해 저장한다`() {
        val retro = inProgressRetrospective()
        val summary = aiSummaryResponse()
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(userFinder.getJobByUserId(userId)).thenReturn(Job.DEVELOPER)
        whenever(aiClient.generateSummaryWithTitle(any(), any(), anyOrNull())).thenReturn(summary)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.complete(retrospectiveId, userId)

        assertThat(result.title).isEqualTo(summary.title)
        assertThat(retro.summary).isNotNull()
        assertThat(retro.inputTokens).isEqualTo(100)
        assertThat(retro.outputTokens).isEqualTo(50)
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
    fun `save - 제목으로 회고를 완료 처리하고 이벤트를 발행한다`() {
        val retro = inProgressRetrospective().apply { saveSummary(summaryFixture()) }
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectService.save(retrospectiveId, userId, "오늘의 회고")

        assertThat(result.isCompleted()).isTrue()
        assertThat(result.title).isEqualTo("오늘의 회고")
        verify(eventPublisher).publishEvent(any<RetrospectiveCompletedEvent>())
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
    fun `save - summary 없이 저장 시 예외가 발생한다`() {
        val retro = inProgressRetrospective()
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        assertThrows<SummaryNotGeneratedException> {
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

    @Test
    fun `registerProject - 정상적으로 프로젝트를 회고에 할당한다`() {
        val retro = inProgressRetrospective()
        val projectId = UUID.randomUUID()
        val project =
            com.didit.domain.organization.Project
                .create(userId, "프로젝트")

        whenever(retrospectiveRepository.findByIdAndDeletedAtIsNull(retrospectiveId)).thenReturn(retro)
        whenever(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId)).thenReturn(project)
        whenever(retrospectiveRepository.save(any())).thenAnswer { it.arguments[0] }

        retrospectService.registerProject(userId, retrospectiveId, projectId)

        assertThat(retro.projectId).isEqualTo(projectId)
        verify(retrospectiveRepository).save(retro)
    }

    @Test
    fun `registerProject - 회고가 존재하지 않으면 RetrospectiveNotFoundException`() {
        val projectId = UUID.randomUUID()

        whenever(retrospectiveRepository.findByIdAndDeletedAtIsNull(retrospectiveId)).thenReturn(null)

        assertThrows<RetrospectiveNotFoundException> {
            retrospectService.registerProject(userId, retrospectiveId, projectId)
        }
        verify(retrospectiveRepository, never()).save(any())
    }

    @Test
    fun `registerProject - 프로젝트가 존재하지 않으면 ProjectNotFoundException`() {
        val retro = inProgressRetrospective()
        val projectId = UUID.randomUUID()

        whenever(retrospectiveRepository.findByIdAndDeletedAtIsNull(retrospectiveId)).thenReturn(retro)
        whenever(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId)).thenReturn(null)

        assertThrows<com.didit.application.organization.exception.ProjectNotFoundException> {
            retrospectService.registerProject(userId, retrospectiveId, projectId)
        }
        verify(retrospectiveRepository, never()).save(any())
    }
}
