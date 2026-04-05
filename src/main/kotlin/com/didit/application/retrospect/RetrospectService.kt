package com.didit.application.retrospect

import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import com.didit.application.auth.provided.UserFinder
import com.didit.application.organization.exception.ProjectNotFoundException
import com.didit.application.organization.required.ProjectRepository
import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import com.didit.application.retrospect.exception.DailyLimitExceededException
import com.didit.application.retrospect.exception.RetrospectiveAlreadyCompletedException
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.exception.RetrospectiveNotInProgressException
import com.didit.application.retrospect.exception.SpeechEmptyFileException
import com.didit.application.retrospect.exception.SpeechEmptyResultException
import com.didit.application.retrospect.exception.SpeechUnsupportedFileException
import com.didit.application.retrospect.exception.SummaryNotGeneratedException
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.application.retrospect.provided.RetrospectiveRegister
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.application.retrospect.required.SpeechClient
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveCompletedEvent
import com.didit.domain.retrospect.RetrospectiveSummary
import com.didit.domain.retrospect.Sender
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrospectService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val retrospectiveFinder: RetrospectiveFinder,
    private val speechClient: SpeechClient,
    private val aiClient: AIClient,
    private val userFinder: UserFinder,
    private val eventPublisher: ApplicationEventPublisher,
    private val auditLogger: AuditLogger,
    private val projectRepository: ProjectRepository,
) : RetrospectiveRegister {
    companion object {
        private val logger = LoggerFactory.getLogger(RetrospectService::class.java)
        private const val DAILY_LIMIT = 3
        private const val DEEP_QUESTION_GENERATING_MESSAGE = "심화 질문을 생성 중입니다."
        private const val DEFAULT_FALLBACK_QUESTION = "오늘 회고를 통해 어떤 성찰을 얻으셨나요?"

        private val QUESTION_CONTENTS =
            mapOf(
                QuestionType.Q1 to "오늘 어떤 일을 하셨나요?",
                QuestionType.Q2 to "진행하면서 어떤 시도, 혹은 어려움이 있었나요?",
                QuestionType.Q3 to "이번 일을 통해 새롭게 느끼거나 배운 점이 있나요?",
            )
    }

    @Transactional
    override fun start(userId: UUID): Retrospective {
        val todayCount = retrospectiveFinder.countByUserIdAndDate(userId, LocalDate.now())

        if (todayCount >= DAILY_LIMIT) throw DailyLimitExceededException(userId)

        val retrospective = Retrospective.create(userId)
        retrospective.addMessage(
            ChatMessage.question(
                retrospective = retrospective,
                content = QUESTION_CONTENTS[QuestionType.Q1]!!,
                questionType = QuestionType.Q1,
            ),
        )

        val saved = retrospectiveRepository.save(retrospective)

        logger.info("회고 시작 - userId: $userId, retrospectiveId: ${saved.id}")

        return saved
    }

    @Transactional
    override fun submitAnswer(
        retrospectiveId: UUID,
        userId: UUID,
        content: String,
    ): SubmitAnswerResponse = processAnswer(retrospectiveId, userId, content, InputType.TEXT)

    @Transactional
    override fun submitVoiceAnswer(
        retrospectiveId: UUID,
        userId: UUID,
        audioBytes: ByteArray,
        filename: String,
    ): SubmitAnswerResponse {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        validateRetrospectiveInProgress(retrospective, retrospectiveId)

        val currentQuestionType =
            retrospective.currentQuestionType()
                ?: throw RetrospectiveNotInProgressException(retrospectiveId)

        if (retrospective.isPending()) retrospective.startProgress()

        val content = transcribe(audioBytes, filename)
        saveUserAnswer(retrospective, content, currentQuestionType, InputType.STT)

        return routeAnswer(retrospective, currentQuestionType).copy(content = content)
    }

    @Async
    @Transactional
    fun generateDeepQuestionAsync(
        retrospectiveId: UUID,
        userId: UUID,
    ) {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        val job = userFinder.getJobByUserId(retrospective.userId)
        val answers = retrospective.getAnswersUpToQ3()

        val deepQuestion =
            try {
                aiClient.generateDeepQuestion(job, answers)
            } catch (e: Exception) {
                logger.error("심화 질문 생성 실패 - userId: $userId, retrospectiveId: $retrospectiveId", e)
                null
            }

        retrospective.addMessage(
            ChatMessage.question(
                retrospective = retrospective,
                content = deepQuestion?.content ?: DEFAULT_FALLBACK_QUESTION,
                questionType = QuestionType.Q4_DEEP,
            ),
        )

        if (deepQuestion != null) {
            retrospective.addTokens(deepQuestion.inputTokens, deepQuestion.outputTokens)
        }

        retrospectiveRepository.save(retrospective)
    }

    @Transactional
    override fun skipDeepQuestion(
        retrospectiveId: UUID,
        userId: UUID,
    ) {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        if (!retrospective.isInProgress()) throw RetrospectiveNotInProgressException(retrospectiveId)

        retrospective.addMessage(
            ChatMessage.skippedAnswer(
                retrospective = retrospective,
                questionType = QuestionType.Q4_DEEP,
            ),
        )
        retrospectiveRepository.save(retrospective)

        logger.info("심화 질문 스킵 - userId: $userId, retrospectiveId: $retrospectiveId")
    }

    @Transactional
    override fun complete(
        retrospectiveId: UUID,
        userId: UUID,
    ): AISummaryResponse {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        validateRetrospectiveInProgress(retrospective, retrospectiveId)

        val job = userFinder.getJobByUserId(userId)

        val summary =
            try {
                aiClient.generateSummaryWithTitle(job, retrospective.getAllAnswers())
            } catch (e: Exception) {
                logger.error("회고 요약 생성 실패 - userId: $userId, retrospectiveId: $retrospectiveId", e)
                throw e
            }

        retrospective.saveSummary(
            RetrospectiveSummary(
                feedback = summary.feedback,
                insight = summary.insight,
                doneWork = summary.doneWork,
                summary = summary.summary,
                blockedPoint = summary.blockedPoint.joinToString("\n"),
                solutionProcess = summary.solutionProcess.joinToString("\n"),
                lessonLearned = summary.lessonLearned.joinToString("\n"),
                nextAction = summary.nextAction.joinToString("\n"),
            ),
        )
        retrospective.addTokens(summary.inputTokens, summary.outputTokens)

        retrospectiveRepository.save(retrospective)

        logger.info("회고 완료 - userId: $userId, retrospectiveId: $retrospectiveId")

        return summary
    }

    @Transactional
    override fun save(
        retrospectiveId: UUID,
        userId: UUID,
        title: String,
    ): Retrospective {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        if (retrospective.isCompleted()) throw RetrospectiveAlreadyCompletedException(retrospectiveId)
        if (retrospective.summary == null) throw SummaryNotGeneratedException(retrospectiveId)

        retrospective.complete(title = title)
        val saved = retrospectiveRepository.save(retrospective)

        eventPublisher.publishEvent(
            RetrospectiveCompletedEvent(
                userId = userId,
                retroDate = LocalDate.now(),
            ),
        )

        logger.info("회고 저장 - userId: $userId, retrospectiveId: $retrospectiveId, title: $title")

        auditLogger.log(
            actorId = userId,
            actorType = ActorType.USER,
            action = AuditAction.RETROSPECTIVE_SAVED,
            targetId = retrospectiveId,
            targetType = "RETROSPECTIVE",
            payload = mapOf("title" to title),
        )

        return saved
    }

    @Transactional
    override fun restart(
        retrospectiveId: UUID,
        userId: UUID,
    ): Retrospective {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)

        retrospective.softDelete()

        retrospectiveRepository.save(retrospective)

        logger.info("회고 다시 시작 - userId: $userId, retrospectiveId: $retrospectiveId")

        return start(userId)
    }

    @Transactional
    override fun updateTitle(
        retrospectiveId: UUID,
        userId: UUID,
        title: String,
    ) {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)

        retrospective.updateTitle(title)

        retrospectiveRepository.save(retrospective)

        logger.info("회고 제목 수정 - userId: $userId, retrospectiveId: $retrospectiveId, title: $title")
    }

    @Transactional
    override fun delete(
        retrospectiveId: UUID,
        userId: UUID,
    ) {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)

        retrospective.softDelete()

        retrospectiveRepository.save(retrospective)

        logger.info("회고 삭제 - userId: $userId, retrospectiveId: $retrospectiveId")
    }

    @Transactional
    override fun exit(
        retrospectiveId: UUID,
        userId: UUID,
    ) {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)

        if (!retrospective.isPending()) return

        retrospective.softDelete()

        retrospectiveRepository.save(retrospective)

        logger.info("회고 나가기 - userId: $userId, retrospectiveId: $retrospectiveId")
    }

    @Transactional
    override fun assignProject(
        userId: UUID,
        retrospectiveId: UUID,
        projectId: UUID,
    ) {
        val retrospective =
            retrospectiveRepository.findByIdAndDeletedAtIsNull(retrospectiveId)
                ?: throw RetrospectiveNotFoundException(
                    retrospectiveId,
                )

        projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId) ?: throw ProjectNotFoundException(
            projectId,
        )

        retrospective.assignProject(projectId)
        retrospectiveRepository.save(retrospective)
    }

    private fun processAnswer(
        retrospectiveId: UUID,
        userId: UUID,
        content: String,
        inputType: InputType,
    ): SubmitAnswerResponse {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        validateRetrospectiveInProgress(retrospective, retrospectiveId)

        val currentQuestionType =
            retrospective.currentQuestionType()
                ?: throw RetrospectiveNotInProgressException(retrospectiveId)

        if (retrospective.isPending()) retrospective.startProgress()

        saveUserAnswer(retrospective, content, currentQuestionType, inputType)

        return routeAnswer(retrospective, currentQuestionType)
    }

    private fun routeAnswer(
        retrospective: Retrospective,
        currentQuestionType: QuestionType,
    ): SubmitAnswerResponse =
        when (currentQuestionType) {
            QuestionType.Q3 -> handleQ3Answer(retrospective)
            QuestionType.Q4_DEEP -> handleQ4Answer(retrospective)
            else -> handleRegularAnswer(retrospective, currentQuestionType)
        }

    private fun transcribe(
        audioBytes: ByteArray,
        filename: String,
    ): String {
        if (audioBytes.isEmpty()) throw SpeechEmptyFileException()
        if (!filename.lowercase().endsWith(".wav")) throw SpeechUnsupportedFileException(filename, null)

        val text = speechClient.transcribe(audioBytes, filename).trim()

        if (text.isBlank()) throw SpeechEmptyResultException()

        return text
    }

    private fun validateRetrospectiveInProgress(
        retrospective: Retrospective,
        retrospectiveId: UUID,
    ) {
        if (retrospective.isCompleted()) throw RetrospectiveAlreadyCompletedException(retrospectiveId)
        if (retrospective.isDeleted()) throw RetrospectiveNotInProgressException(retrospectiveId)
    }

    private fun saveUserAnswer(
        retrospective: Retrospective,
        content: String,
        questionType: QuestionType,
        inputType: InputType,
    ) {
        retrospective.addMessage(
            ChatMessage.userAnswer(
                retrospective = retrospective,
                content = content,
                questionType = questionType,
                inputType = inputType,
            ),
        )
        retrospectiveRepository.save(retrospective)
    }

    private fun handleQ3Answer(retrospective: Retrospective): SubmitAnswerResponse {
        generateDeepQuestionAsync(retrospective.id, retrospective.userId)
        return SubmitAnswerResponse(
            nextQuestionType = QuestionType.Q4_DEEP,
            nextQuestionContent = DEEP_QUESTION_GENERATING_MESSAGE,
            isReadyToComplete = false,
        )
    }

    private fun handleQ4Answer(retrospective: Retrospective): SubmitAnswerResponse {
        val deepQuestion =
            retrospective.chatMessages
                .find { it.questionType == QuestionType.Q4_DEEP && it.sender == Sender.AI }
                ?: return SubmitAnswerResponse(
                    nextQuestionType = QuestionType.Q4_DEEP,
                    nextQuestionContent = DEEP_QUESTION_GENERATING_MESSAGE,
                    isReadyToComplete = false,
                )

        return SubmitAnswerResponse(
            nextQuestionType = QuestionType.Q4_DEEP,
            nextQuestionContent = deepQuestion.content,
            isReadyToComplete = true,
        )
    }

    private fun handleRegularAnswer(
        retrospective: Retrospective,
        currentQuestionType: QuestionType,
    ): SubmitAnswerResponse {
        val nextQuestionType = QuestionType.entries[currentQuestionType.ordinal + 1]
        val nextContent = QUESTION_CONTENTS[nextQuestionType]!!

        retrospective.addMessage(
            ChatMessage.question(
                retrospective = retrospective,
                content = nextContent,
                questionType = nextQuestionType,
            ),
        )

        retrospectiveRepository.save(retrospective)

        return SubmitAnswerResponse(
            nextQuestionType = nextQuestionType,
            nextQuestionContent = nextContent,
            isReadyToComplete = false,
        )
    }
}
