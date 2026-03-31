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
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveSummary
import com.didit.domain.retrospect.Sender
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
        inputType: InputType,
    ): SubmitAnswerResponse {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        if (retrospective.isCompleted()) throw RetrospectiveAlreadyCompletedException(retrospectiveId)
        if (retrospective.isDeleted()) throw RetrospectiveNotInProgressException(retrospectiveId)

        val currentQuestionType =
            retrospective.currentQuestionType()
                ?: throw RetrospectiveNotInProgressException(retrospectiveId)

        // Q1 첫 답변 시 PENDING → IN_PROGRESS 전환
        if (retrospective.isPending()) {
            retrospective.startProgress()
        }

        retrospective.addMessage(
            ChatMessage.userAnswer(
                retrospective = retrospective,
                content = content,
                questionType = currentQuestionType,
                inputType = inputType,
            ),
        )
        retrospectiveRepository.save(retrospective)

        if (currentQuestionType == QuestionType.Q3) {
            // 심화 질문을 동기로 생성
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

            return SubmitAnswerResponse(
                nextQuestionType = QuestionType.Q4_DEEP,
                nextQuestionContent = deepQuestionContent,
                isReadyToComplete = false,
            )
        }

        if (currentQuestionType == QuestionType.Q4_DEEP) {
            // 심화 질문이 이미 생성되었는지 확인
            val deepQuestion =
                retrospective.chatMessages
                    .find { it.questionType == QuestionType.Q4_DEEP && it.sender == Sender.AI }

            return if (deepQuestion != null) {
                SubmitAnswerResponse(
                    nextQuestionType = QuestionType.Q4_DEEP,
                    nextQuestionContent = deepQuestion.content,
                    isReadyToComplete = false,
                )
            } else {
                // 심화 질문이 아직 생성되지 않았다면 잠시 기다렸다가 다시 확인
                Thread.sleep(1000) // 1초 대기
                val retryQuestion =
                    retrospective.chatMessages
                        .find { it.questionType == QuestionType.Q4_DEEP && it.sender == Sender.AI }

                if (retryQuestion != null) {
                    SubmitAnswerResponse(
                        nextQuestionType = QuestionType.Q4_DEEP,
                        nextQuestionContent = retryQuestion.content,
                        isReadyToComplete = false,
                    )
                } else {
                    SubmitAnswerResponse(
                        nextQuestionType = QuestionType.Q4_DEEP,
                        nextQuestionContent = "심화 질문을 생성 중입니다...",
                        isReadyToComplete = false,
                    )
                }
            }
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
}
