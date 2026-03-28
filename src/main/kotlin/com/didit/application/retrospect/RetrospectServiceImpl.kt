package com.didit.application.retrospect

import com.didit.application.retrospect.dto.StartRetrospectiveResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import com.didit.application.retrospect.provided.RetrospectService
import com.didit.application.retrospect.required.AIClient
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.application.retrospect.required.RetrospectiveSummaryRepository
import com.didit.application.retrospect.required.UserFinder
import com.didit.domain.auth.Job
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveSummary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class RetrospectServiceImpl(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val retrospectiveSummaryRepository: RetrospectiveSummaryRepository,
    private val userFinder: UserFinder,
    private val aiClient: AIClient,
) : RetrospectService {
    companion object {
        private val COMMON_QUESTIONS =
            mapOf(
                1 to "오늘 어떤 일을 하셨나요?",
                2 to "진행하면서 어떤 시도, 혹은 어려움이 있었나요?",
                3 to "이번 일을 통해 새롭게 느끼거나 배운 점이 있나요?",
            )
    }

    @Transactional
    override fun startRetrospective(userId: UUID): StartRetrospectiveResponse {
        val user = userFinder.findByIdOrThrow(userId)

        val existingRetrospective = retrospectiveRepository.findByUserId(userId)
        if (existingRetrospective != null && !existingRetrospective.isCompleted) {
            val currentQuestion =
                COMMON_QUESTIONS[existingRetrospective.currentQuestionNumber]
                    ?: throw IllegalStateException("잘못된 질문 번호입니다.")

            return StartRetrospectiveResponse(
                retrospectiveId = existingRetrospective.id.toString(),
                questionNumber = existingRetrospective.currentQuestionNumber,
                question = currentQuestion,
                isDeepQuestion = false,
            )
        }

        val retrospective = Retrospective.create(userId, user.job ?: Job.DEVELOPER)
        val firstQuestion = COMMON_QUESTIONS[retrospective.currentQuestionNumber]!!

        val questionMessage =
            ChatMessage.createQuestion(
                retrospective = retrospective,
                questionNumber = retrospective.currentQuestionNumber,
                content = firstQuestion,
            )
        retrospective.addChatMessage(questionMessage)

        val savedRetrospective = retrospectiveRepository.save(retrospective)

        return StartRetrospectiveResponse(
            retrospectiveId = savedRetrospective.id.toString(),
            questionNumber = savedRetrospective.currentQuestionNumber,
            question = firstQuestion,
            isDeepQuestion = false,
        )
    }

    @Transactional
    override fun submitAnswer(
        userId: UUID,
        answer: String,
    ): SubmitAnswerResponse {
        val retrospective =
            retrospectiveRepository.findByUserIdWithChatMessages(userId)
                ?: throw IllegalArgumentException("진행 중인 회고가 없습니다.")

        if (retrospective.isCompleted) {
            throw IllegalStateException("이미 완료된 회고입니다.")
        }

        val answerMessage =
            ChatMessage.createAnswer(
                retrospective = retrospective,
                questionNumber = retrospective.currentQuestionNumber,
                content = answer,
            )
        retrospective.addChatMessage(answerMessage)

        return when {
            retrospective.currentQuestionNumber < 3 -> {
                // 답변 저장 후 AI 호출 (미리 다음 질문 준비)
                val answers = retrospective.getAnswersForDeepQuestion()
                aiClient.generateDeepQuestion(retrospective.userJob, answers) // AI 호출 (결과는 저장하지 않음)

                retrospective.moveToNextQuestion()
                val nextQuestion = COMMON_QUESTIONS[retrospective.currentQuestionNumber]!!

                val nextQuestionMessage =
                    ChatMessage.createQuestion(
                        retrospective = retrospective,
                        questionNumber = retrospective.currentQuestionNumber,
                        content = nextQuestion,
                    )
                retrospective.addChatMessage(nextQuestionMessage)
                retrospectiveRepository.save(retrospective)

                SubmitAnswerResponse(
                    retrospectiveId = retrospective.id.toString(),
                    questionNumber = retrospective.currentQuestionNumber,
                    nextQuestion = nextQuestion,
                    isDeepQuestion = false,
                    isCompleted = false,
                )
            }

            retrospective.currentQuestionNumber == 3 -> {
                // 답변 저장 후 AI 호출 (Deep Question 생성)
                val answers = retrospective.getAnswersForDeepQuestion()
                val deepQuestion = aiClient.generateDeepQuestion(retrospective.userJob, answers)

                retrospective.moveToNextQuestion()

                val deepQuestionMessage =
                    ChatMessage.createQuestion(
                        retrospective = retrospective,
                        questionNumber = 4,
                        content = deepQuestion,
                        isDeepQuestion = true,
                    )
                retrospective.addChatMessage(deepQuestionMessage)
                retrospectiveRepository.save(retrospective)

                SubmitAnswerResponse(
                    retrospectiveId = retrospective.id.toString(),
                    questionNumber = 4,
                    nextQuestion = deepQuestion,
                    isDeepQuestion = true,
                    isCompleted = false,
                )
            }

            retrospective.currentQuestionNumber == 4 -> {
                retrospective.complete()
                retrospectiveRepository.save(retrospective)

                val allAnswers = retrospective.getAllAnswers()
                val summary = aiClient.generateSummary(retrospective.userJob, allAnswers)

                val retrospectiveSummary =
                    RetrospectiveSummary.create(
                        retrospective = retrospective,
                        summaryContent = summary,
                    )
                retrospectiveSummaryRepository.save(retrospectiveSummary)

                SubmitAnswerResponse(
                    retrospectiveId = retrospective.id.toString(),
                    questionNumber = 4,
                    nextQuestion = null,
                    isDeepQuestion = false,
                    isCompleted = true,
                    summary = summary,
                )
            }

            else -> throw IllegalStateException("잘못된 질문 번호입니다.")
        }
    }
}
