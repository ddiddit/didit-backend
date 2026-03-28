package com.didit.domain.retrospect

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "chat_messages")
@Entity
class ChatMessage(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retrospective_id", nullable = false)
    val retrospective: Retrospective,
    @Column(nullable = false)
    val questionNumber: Int,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Column(name = "is_answer", nullable = false)
    val isAnswer: Boolean,
    @Column(name = "is_deep_question", nullable = false)
    val isDeepQuestion: Boolean = false,
    @Column(name = "message_created_at", nullable = false)
    val messageCreatedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity() {
    companion object {
        fun createQuestion(
            retrospective: Retrospective,
            questionNumber: Int,
            content: String,
            isDeepQuestion: Boolean = false,
        ): ChatMessage =
            ChatMessage(
                retrospective = retrospective,
                questionNumber = questionNumber,
                content = content,
                isAnswer = false,
                isDeepQuestion = isDeepQuestion,
            )

        fun createAnswer(
            retrospective: Retrospective,
            questionNumber: Int,
            content: String,
        ): ChatMessage =
            ChatMessage(
                retrospective = retrospective,
                questionNumber = questionNumber,
                content = content,
                isAnswer = true,
            )
    }
}
