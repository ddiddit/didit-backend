package com.didit.application.retrospect

import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import com.didit.application.retrospect.exception.DailyLimitExceededException
import com.didit.application.retrospect.exception.RetrospectiveAlreadyCompletedException
import com.didit.application.retrospect.exception.RetrospectiveNotInProgressException
import com.didit.application.retrospect.exception.SpeechEmptyFileException
import com.didit.application.retrospect.exception.SpeechEmptyResultException
import com.didit.application.retrospect.exception.SpeechUnsupportedFileException
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.application.retrospect.provided.RetrospectiveRegister
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.application.retrospect.required.SpeechClient
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveSummary
import com.didit.domain.retrospect.Sender
import org.slf4j.LoggerFactory
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
        return retrospectiveRepository.save(retrospective)
    }

    @Transactional
    override fun submitTextAnswer(
        retrospectiveId: UUID,
        userId: UUID,
        content: String,
    ): SubmitAnswerResponse = submitAnswer(retrospectiveId, userId, content, InputType.TEXT)

    @Transactional
    override fun submitVoiceAnswer(
        retrospectiveId: UUID,
        userId: UUID,
        audioBytes: ByteArray,
        filename: String,
    ): SubmitAnswerResponse {
        val content = transcribe(audioBytes, filename)
        return submitAnswer(retrospectiveId, userId, content, InputType.STT)
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

        val deepQuestionContent =
            try {
                aiClient.generateDeepQuestion(job, answers)
            } catch (e: Exception) {
                logger.error("Failed to generate deep question for userId: $userId, retrospectiveId: $retrospectiveId", e)
                DEFAULT_FALLBACK_QUESTION
            }

        retrospective.addMessage(
            ChatMessage.question(
                retrospective = retrospective,
                content = deepQuestionContent,
                questionType = QuestionType.Q4_DEEP,
            ),
        )
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
                logger.error("Failed to generate summary", e)
                throw e
            }

        retrospective.saveSummary(
            RetrospectiveSummary(
                feedback = summary.feedback,
                insight = summary.insight,
                doneWork = summary.doneWork,
                blockedPoint = summary.blockedPoint,
                solutionProcess = summary.solutionProcess,
                lessonLearned = summary.lessonLearned,
            ),
        )
        retrospectiveRepository.save(retrospective)
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

        retrospective.complete(title = title)
        return retrospectiveRepository.save(retrospective)
    }

    @Transactional
    override fun restart(
        retrospectiveId: UUID,
        userId: UUID,
    ): Retrospective {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        retrospective.softDelete()
        retrospectiveRepository.save(retrospective)
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
    }

    @Transactional
    override fun delete(
        retrospectiveId: UUID,
        userId: UUID,
    ) {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        retrospective.softDelete()
        retrospectiveRepository.save(retrospective)
    }

    @Transactional
    override fun exit(
        retrospectiveId: UUID,
        userId: UUID,
    ) {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        if (retrospective.isPending()) {
            retrospective.softDelete()
            retrospectiveRepository.save(retrospective)
        }
    }

    private fun submitAnswer(
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

        return when (currentQuestionType) {
            QuestionType.Q3 -> handleQ3Answer(retrospective)
            QuestionType.Q4_DEEP -> handleQ4Answer(retrospective)
            else -> handleRegularAnswer(retrospective, currentQuestionType)
        }
    }

    private fun transcribe(
        audioBytes: ByteArray,
        filename: String,
    ): String {
        if (audioBytes.isEmpty()) throw SpeechEmptyFileException()
        if (!filename.lowercase().endsWith(".wav")) {
            throw SpeechUnsupportedFileException(filename, null)
        }

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

        return if (deepQuestion != null) {
            SubmitAnswerResponse(
                nextQuestionType = QuestionType.Q4_DEEP,
                nextQuestionContent = deepQuestion.content,
                isReadyToComplete = true,
            )
        } else {
            SubmitAnswerResponse(
                nextQuestionType = QuestionType.Q4_DEEP,
                nextQuestionContent = DEEP_QUESTION_GENERATING_MESSAGE,
                isReadyToComplete = false,
            )
        }
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
