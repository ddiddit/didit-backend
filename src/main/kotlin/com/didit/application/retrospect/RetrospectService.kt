package com.didit.application.retrospect

import com.didit.application.auth.provided.UserFinder
import com.didit.application.retrospect.dto.AISummaryResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import com.didit.application.retrospect.exception.DailyLimitExceededException
import com.didit.application.retrospect.exception.RetrospectiveAlreadyCompletedException
import com.didit.application.retrospect.exception.RetrospectiveNotInProgressException
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.application.retrospect.provided.RetrospectiveRegister
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveSummary
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
    private val aiClient: AIClient,
    private val userFinder: UserFinder,
) : RetrospectiveRegister {
    companion object {
        private const val DAILY_LIMIT = 3
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
    override fun submitAnswer(
        retrospectiveId: UUID,
        userId: UUID,
        content: String,
    ): SubmitAnswerResponse {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        if (!retrospective.isInProgress()) throw RetrospectiveNotInProgressException(retrospectiveId)

        val currentQuestionType =
            retrospective.currentQuestionType()
                ?: throw RetrospectiveNotInProgressException(retrospectiveId)

        retrospective.addMessage(
            ChatMessage.userAnswer(
                retrospective = retrospective,
                content = content,
                questionType = currentQuestionType,
            ),
        )
        retrospectiveRepository.save(retrospective)

        if (currentQuestionType == QuestionType.Q3) {
            generateDeepQuestionAsync(retrospective.id, userId)
            return SubmitAnswerResponse(
                nextQuestionType = QuestionType.Q4_DEEP,
                nextQuestionContent = null,
                isReadyToComplete = false,
            )
        }

        if (currentQuestionType == QuestionType.Q4_DEEP) {
            return SubmitAnswerResponse(
                nextQuestionType = null,
                nextQuestionContent = null,
                isReadyToComplete = true,
            )
        }

        val nextQuestionType = QuestionType.entries[currentQuestionType.ordinal + 1]
        val nextContent = QUESTION_CONTENTS[nextQuestionType]!!

        retrospective.addMessage(
            ChatMessage.question(
                retrospective = retrospective,
                content = nextContent,
                questionType = nextQuestionType,
            ),
        )

        return SubmitAnswerResponse(
            nextQuestionType = nextQuestionType,
            nextQuestionContent = nextContent,
            isReadyToComplete = false,
        )
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
        val deepQuestionContent = aiClient.generateDeepQuestion(job, answers)

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

        if (retrospective.isCompleted()) throw RetrospectiveAlreadyCompletedException(retrospectiveId)
        if (!retrospective.isInProgress()) throw RetrospectiveNotInProgressException(retrospectiveId)

        val job = userFinder.getJobByUserId(userId)

        return aiClient.generateSummaryWithTitle(job, retrospective.getAllAnswers())
    }

    @Transactional
    override fun save(
        retrospectiveId: UUID,
        userId: UUID,
        title: String,
        projectId: UUID?,
        summary: AISummaryResponse,
    ): Retrospective {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)

        if (retrospective.isCompleted()) throw RetrospectiveAlreadyCompletedException(retrospectiveId)

        retrospective.complete(
            title = title,
            summary =
                RetrospectiveSummary(
                    feedback = summary.feedback,
                    insight = summary.insight,
                    doneWork = summary.doneWork,
                    blockedPoint = summary.blockedPoint,
                    solutionProcess = summary.solutionProcess,
                    lessonLearned = summary.lessonLearned,
                ),
            inputTokens = 0,
            outputTokens = 0,
        )

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
}
