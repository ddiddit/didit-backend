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
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    val inputType: InputType? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val messageType: ConversationMessageType = ConversationMessageType.CONVERSATION,
    @Column(columnDefinition = "TEXT")
    val supportingContent: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var relevance: MessageRelevance? = null,
    @Column(nullable = false)
    var includedInResult: Boolean = true,
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
                inputType = null,
            )

        fun userAnswer(
            retrospective: Retrospective,
            content: String,
            questionType: QuestionType,
            inputType: InputType,
        ): ChatMessage {
            require(content.isNotBlank()) { "답변 내용은 비어 있을 수 없습니다." }

            return ChatMessage(
                retrospective = retrospective,
                sender = Sender.USER,
                content = content,
                questionType = questionType,
                inputType = inputType,
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
                inputType = null,
            )
        }

        fun v2Intro(retrospective: Retrospective): ChatMessage =
            ChatMessage(
                retrospective = retrospective,
                sender = Sender.AI,
                content = "오늘 어떤 일을 하셨나요?",
                questionType = QuestionType.V2_CHAT,
                messageType = ConversationMessageType.INTRO,
                supportingContent = "오늘 진행한 일 중 하나를 떠올려, 작업 내용과 함께 결과나 상태도 같이 적어보세요.",
                includedInResult = false,
            )

        fun v2UserMessage(
            retrospective: Retrospective,
            content: String,
        ): ChatMessage {
            require(content.isNotBlank()) { "회고 내용은 비어 있을 수 없습니다." }
            return ChatMessage(
                retrospective = retrospective,
                sender = Sender.USER,
                content = content,
                questionType = QuestionType.V2_CHAT,
                inputType = InputType.TEXT,
                messageType = ConversationMessageType.CONVERSATION,
                includedInResult = false,
            )
        }

        fun v2AssistantMessage(
            retrospective: Retrospective,
            content: String,
            systemGuide: Boolean = false,
        ): ChatMessage =
            ChatMessage(
                retrospective = retrospective,
                sender = Sender.AI,
                content = content,
                questionType = QuestionType.V2_CHAT,
                messageType =
                    if (systemGuide) ConversationMessageType.SYSTEM_GUIDE else ConversationMessageType.CONVERSATION,
                includedInResult = false,
            )
    }

    fun classify(relevance: MessageRelevance) {
        this.relevance = relevance
        includedInResult = relevance == MessageRelevance.RETROSPECTIVE
    }
}
