package com.didit.domain.retrospect.entity

import com.didit.domain.retrospect.enums.QuestionType
import com.didit.domain.retrospect.enums.RetroStatus
import com.didit.domain.retrospect.model.RetrospectiveSummary
import java.util.UUID

data class Retrospective(
    val id: UUID,
    val userId: UUID,
    val projectId: UUID?,
    val tagIds: MutableList<UUID> = mutableListOf(),
    var title: String? = null,
    var status: RetroStatus = RetroStatus.IN_PROGRESS,
    var inputTokens: Int = 0,
    var outputTokens: Int = 0,
    val chatMessages: MutableList<ChatMessage> = mutableListOf(),
    var summary: RetrospectiveSummary? = null,
) {
    fun addUserAnswer(
        questionType: QuestionType,
        content: String,
    ) {
        validateNotCompleted()
        chatMessages.add(ChatMessage.user(questionType, content))
    }

    fun addAiQuestion(
        questionType: QuestionType,
        content: String,
    ) {
        chatMessages.add(ChatMessage.ai(questionType, content))
    }

    fun currentQuestionType(): QuestionType? {
        val answeredTypes =
            chatMessages
                .filter { it.isUserMessage() }
                .map { it.questionType }
                .toSet()

        return when {
            QuestionType.Q1 !in answeredTypes -> QuestionType.Q1
            QuestionType.Q2 !in answeredTypes -> QuestionType.Q2
            QuestionType.Q3 !in answeredTypes -> QuestionType.Q3
            QuestionType.Q4_DEEP !in answeredTypes -> QuestionType.Q4_DEEP
            else -> null
        }
    }

    fun complete(summary: RetrospectiveSummary) {
        validateNotCompleted()
        this.summary = summary
        this.status = RetroStatus.COMPLETED
    }

    fun accumulateTokens(
        input: Int,
        output: Int,
    ) {
        inputTokens += input
        outputTokens += output
    }

    fun getMessagesForAi(): List<ChatMessage> = chatMessages.toList()

    fun validateNotCompleted() {
        require(status != RetroStatus.COMPLETED) { "이미 완료된 회고입니다." }
    }
}
