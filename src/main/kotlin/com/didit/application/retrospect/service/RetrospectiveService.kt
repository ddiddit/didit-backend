package com.didit.application.retrospect.service

import com.didit.application.retrospect.dto.command.StartRetrospectiveCommand
import com.didit.application.retrospect.dto.command.SubmitAnswerCommand
import com.didit.application.retrospect.dto.result.RetrospectiveResult
import com.didit.application.retrospect.dto.result.RetrospectiveSummaryResult
import com.didit.application.retrospect.dto.result.StartRetrospectiveResult
import com.didit.application.retrospect.dto.result.SubmitAnswerResult
import com.didit.application.retrospect.port.inbound.GetRetrospectiveResultUseCase
import com.didit.application.retrospect.port.inbound.StartRetrospectiveUseCase
import com.didit.application.retrospect.port.inbound.SubmitAnswerUseCase
import com.didit.application.retrospect.port.out.RetrospectiveAiPort
import com.didit.application.retrospect.port.out.RetrospectiveCommandPort
import com.didit.application.retrospect.port.out.RetrospectiveQueryPort
import com.didit.domain.retrospect.entity.Retrospective
import com.didit.domain.retrospect.enums.QuestionType
import com.didit.domain.retrospect.service.FixedQuestionProvider
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RetrospectiveService(
    private val retrospectiveCommandPort: RetrospectiveCommandPort,
    private val retrospectiveQueryPort: RetrospectiveQueryPort,
    private val retrospectiveAiPort: RetrospectiveAiPort,
) : StartRetrospectiveUseCase,
    SubmitAnswerUseCase,
    GetRetrospectiveResultUseCase {
    override fun start(command: StartRetrospectiveCommand): StartRetrospectiveResult {
        val retrospective =
            Retrospective(
                id = UUID.randomUUID(),
                userId = command.userId,
                projectId = command.projectId,
                tagIds = command.tagIds.toMutableList(),
            )

        val firstQuestion = FixedQuestionProvider.q1()
        retrospective.addAiQuestion(QuestionType.Q1, firstQuestion)
        retrospectiveCommandPort.save(retrospective)

        return StartRetrospectiveResult(
            retrospectiveId = retrospective.id,
            questionType = QuestionType.Q1,
            question = firstQuestion,
        )
    }

    override fun submitAnswer(command: SubmitAnswerCommand): SubmitAnswerResult {
        val retrospective =
            retrospectiveQueryPort.findById(command.retrospectiveId)
                ?: throw IllegalArgumentException("회고를 찾을 수 없습니다.")

        val currentQuestionType =
            retrospective.currentQuestionType()
                ?: throw IllegalStateException("이미 완료된 회고입니다.")

        retrospective.addUserAnswer(currentQuestionType, command.answer)

        val analyzeResult = retrospectiveAiPort.analyzeAnswer(retrospective.getMessagesForAi())
        retrospective.accumulateTokens(analyzeResult.inputTokens, analyzeResult.outputTokens)

        return when (currentQuestionType) {
            QuestionType.Q1 -> {
                nextFixedQuestion(retrospective, QuestionType.Q2, FixedQuestionProvider.q2())
            }

            QuestionType.Q2 -> {
                nextFixedQuestion(retrospective, QuestionType.Q3, FixedQuestionProvider.q3())
            }

            QuestionType.Q3 -> {
                val deepQuestionResult = retrospectiveAiPort.generateDeepQuestion(retrospective.getMessagesForAi())
                retrospective.accumulateTokens(deepQuestionResult.inputTokens, deepQuestionResult.outputTokens)
                retrospective.addAiQuestion(QuestionType.Q4_DEEP, deepQuestionResult.question)
                retrospectiveCommandPort.save(retrospective)

                SubmitAnswerResult(
                    completed = false,
                    questionType = QuestionType.Q4_DEEP,
                    question = deepQuestionResult.question,
                )
            }

            QuestionType.Q4_DEEP -> {
                val summaryResult = retrospectiveAiPort.generateSummary(retrospective.getMessagesForAi())
                retrospective.accumulateTokens(summaryResult.inputTokens, summaryResult.outputTokens)
                retrospective.complete(summaryResult.summary)
                retrospectiveCommandPort.save(retrospective)

                SubmitAnswerResult(
                    completed = true,
                    summary =
                        RetrospectiveSummaryResult(
                            doneWork = summaryResult.summary.doneWork,
                            blockedPoint = summaryResult.summary.blockedPoint,
                            solutionProcess = summaryResult.summary.solutionProcess,
                            lessonLearned = summaryResult.summary.lessonLearned,
                            insight = summaryResult.summary.insight,
                            improvementDirection = summaryResult.summary.improvementDirection,
                        ),
                )
            }
        }
    }

    override fun getResult(retrospectiveId: UUID): RetrospectiveResult {
        val retrospective =
            retrospectiveQueryPort.findById(retrospectiveId)
                ?: throw IllegalArgumentException("회고를 찾을 수 없습니다.")

        return RetrospectiveResult(
            retrospectiveId = retrospective.id,
            status = retrospective.status,
            summary =
                retrospective.summary?.let {
                    RetrospectiveSummaryResult(
                        doneWork = it.doneWork,
                        blockedPoint = it.blockedPoint,
                        solutionProcess = it.solutionProcess,
                        lessonLearned = it.lessonLearned,
                        insight = it.insight,
                        improvementDirection = it.improvementDirection,
                    )
                },
        )
    }

    private fun nextFixedQuestion(
        retrospective: Retrospective,
        questionType: QuestionType,
        question: String,
    ): SubmitAnswerResult {
        retrospective.addAiQuestion(questionType, question)
        retrospectiveCommandPort.save(retrospective)

        return SubmitAnswerResult(
            completed = false,
            questionType = questionType,
            question = question,
        )
    }
}
