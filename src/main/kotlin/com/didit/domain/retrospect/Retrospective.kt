package com.didit.domain.retrospect

import com.didit.domain.auth.Job
import com.didit.domain.shared.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "retrospectives")
@Entity
class Retrospective(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val userJob: Job,
    @Column(name = "current_question_number", nullable = false)
    var currentQuestionNumber: Int = 1,
    @Column(name = "is_completed", nullable = false)
    var isCompleted: Boolean = false,
    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,
    @OneToMany(mappedBy = "retrospective", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var chatMessages: MutableList<ChatMessage> = mutableListOf(),
) : BaseEntity() {
    fun addChatMessage(message: ChatMessage) {
        chatMessages.add(message)
    }

    fun moveToNextQuestion(): Int {
        currentQuestionNumber++
        return currentQuestionNumber
    }

    fun complete(now: LocalDateTime = LocalDateTime.now()) {
        isCompleted = true
        completedAt = now
    }

    fun canMoveToNextQuestion(): Boolean = currentQuestionNumber < 4

    fun needsDeepQuestion(): Boolean = currentQuestionNumber == 4 && !hasDeepQuestion()

    private fun hasDeepQuestion(): Boolean = chatMessages.any { it.questionNumber == 4 }

    fun getAllAnswers(): List<String> =
        chatMessages
            .filter { it.isAnswer }
            .sortedBy { it.questionNumber }
            .map { it.content }

    fun getAnswersForDeepQuestion(): List<String> =
        chatMessages
            .filter { it.isAnswer && it.questionNumber <= 3 }
            .sortedBy { it.questionNumber }
            .map { it.content }

    companion object {
        fun create(
            userId: UUID,
            userJob: Job,
        ): Retrospective =
            Retrospective(
                userId = userId,
                userJob = userJob,
            )
    }
}
