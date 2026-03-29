package com.didit.domain.retrospect

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "chat_messages")
@Entity
@EntityListeners(AuditingEntityListener::class)
class ChatMessage(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retrospective_id", nullable = false)
    val retrospective: Retrospective,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val sender: Sender,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val questionType: QuestionType,
    @Column(nullable = false)
    val isSkipped: Boolean = false,
) {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    companion object {
        fun question(
            retrospective: Retrospective,
            content: String,
            questionType: QuestionType,
        ): ChatMessage =
            ChatMessage(
                retrospective = retrospective,
                sender = Sender.AI,
                content = content,
                questionType = questionType,
            )

        fun userAnswer(
            retrospective: Retrospective,
            content: String,
            questionType: QuestionType,
        ): ChatMessage {
            require(content.isNotBlank()) { "답변 내용은 비어 있을 수 없습니다." }

            return ChatMessage(
                retrospective = retrospective,
                sender = Sender.USER,
                content = content,
                questionType = questionType,
            )
        }

        fun skippedAnswer(
            retrospective: Retrospective,
            questionType: QuestionType,
        ): ChatMessage {
            require(questionType == QuestionType.Q4_DEEP) { "Q1~Q3는 스킵할 수 없습니다." }

            return ChatMessage(
                retrospective = retrospective,
                sender = Sender.USER,
                content = "",
                questionType = questionType,
                isSkipped = true,
            )
        }
    }
}
