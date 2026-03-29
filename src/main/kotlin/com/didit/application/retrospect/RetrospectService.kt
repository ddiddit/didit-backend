package com.didit.application.retrospect

import com.didit.application.auth.provided.UserFinder
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
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
    private val objectMapper: ObjectMapper,
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

        if (currentQuestionType == QuestionType.Q3) {
            generateDeepQuestionAsync(retrospective)
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
    fun generateDeepQuestionAsync(retrospective: Retrospective) {
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
        title: String,
        projectId: UUID?,
    ): Retrospective {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        if (retrospective.isCompleted()) throw RetrospectiveAlreadyCompletedException(retrospectiveId)
        if (!retrospective.isInProgress()) throw RetrospectiveNotInProgressException(retrospectiveId)

        val job = userFinder.getJobByUserId(userId)
        val allAnswers = retrospective.getAllAnswers()

        val summaryJson = aiClient.generateSummary(job, allAnswers)
        val summary = parseSummary(summaryJson)

        retrospective.complete(
            title = title,
            summary = summary,
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
    override fun delete(
        retrospectiveId: UUID,
        userId: UUID,
    ) {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        retrospective.softDelete()
        retrospectiveRepository.save(retrospective)
    }

    private fun parseSummary(summaryJson: String): RetrospectiveSummary {
        data class SummaryDto(
            val aiFeedback: String = "",
            val insight: String = "",
            val doneWork: String = "",
            val blockedPoint: String = "",
            val solutionProcess: String = "",
            val lessonLearned: String = "",
        )

        val dto =
            runCatching {
                objectMapper.readValue<SummaryDto>(summaryJson)
            }.getOrElse {
                throw RuntimeException("회고 요약 파싱에 실패했습니다. response: $summaryJson")
            }

        return RetrospectiveSummary(
            feedback = dto.aiFeedback,
            insight = dto.insight,
            doneWork = dto.doneWork,
            blockedPoint = dto.blockedPoint,
            solutionProcess = dto.solutionProcess,
            lessonLearned = dto.lessonLearned,
        )
    }
}
