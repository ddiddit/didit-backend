package com.didit.domain.retrospect

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

enum class ConversationTurnStatus {
    PROCESSING,
    COMPLETED,
    FAILED,
}

enum class ConversationTurnAction {
    ASK,
    REFLECT,
    OFFER_COMPLETION,
    CONFIRM_COMPLETION,
}

enum class ConversationAnswerGuideMode {
    INLINE,
    ON_DEMAND,
}

enum class ConversationUserIntent {
    NORMAL,
    CONTINUE_AFTER_COMPLETION,
}

@Table(
    name = "retrospective_analysis_items",
    uniqueConstraints = [UniqueConstraint(columnNames = ["retrospective_id", "item_type"])],
)
@Entity
class RetrospectiveAnalysisItem(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val retrospectiveId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val itemType: RetrospectiveItemType,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: RetrospectiveItemStatus = RetrospectiveItemStatus.EMPTY,
    @Column(columnDefinition = "TEXT")
    var summary: String? = null,
    @Column(nullable = false)
    var questionAllowed: Boolean = true,
) : BaseEntity() {
    fun update(
        newStatus: RetrospectiveItemStatus,
        newSummary: String,
    ) {
        if (newStatus.ordinal >= status.ordinal) status = newStatus
        summary = newSummary.trim().takeIf { it.isNotEmpty() }
        questionAllowed = true
    }

    fun declineQuestion() {
        questionAllowed = false
    }

    companion object {
        fun initialize(retrospectiveId: UUID): List<RetrospectiveAnalysisItem> =
            RetrospectiveItemType.entries.map { RetrospectiveAnalysisItem(retrospectiveId = retrospectiveId, itemType = it) }
    }
}

@Table(
    name = "retrospective_analysis_evidences",
    uniqueConstraints = [UniqueConstraint(columnNames = ["analysis_item_id", "message_id"])],
)
@Entity
@EntityListeners(AuditingEntityListener::class)
class RetrospectiveAnalysisEvidence(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val analysisItemId: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val messageId: UUID,
) {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set
}

@Table(
    name = "retrospective_conversation_turns",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["retrospective_id", "client_message_id"]),
        UniqueConstraint(columnNames = ["retrospective_id", "turn_number"]),
    ],
)
@Entity
class RetrospectiveConversationTurn(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val retrospectiveId: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val clientMessageId: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val userMessageId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val conversationIntent: ConversationUserIntent = ConversationUserIntent.NORMAL,
    @Column(columnDefinition = "BINARY(16)")
    var assistantMessageId: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var questionTarget: RetrospectiveItemType? = null,
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    var action: ConversationTurnAction? = null,
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var answerGuideMode: ConversationAnswerGuideMode? = null,
    @Convert(converter = StringListJsonConverter::class)
    @Column(columnDefinition = "TEXT")
    var answerExamples: List<String> = emptyList(),
    @Column(nullable = false)
    val turnNumber: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ConversationTurnStatus = ConversationTurnStatus.PROCESSING,
    @Column(nullable = false)
    var attemptCount: Int = 1,
    @Column(length = 50)
    var errorCode: String? = null,
    @Column(nullable = false)
    var inputTokens: Int = 0,
    @Column(nullable = false)
    var outputTokens: Int = 0,
    @Column
    var completedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun retry() {
        check(status == ConversationTurnStatus.FAILED)
        status = ConversationTurnStatus.PROCESSING
        attemptCount += 1
        errorCode = null
        completedAt = null
    }

    fun complete(
        assistantMessageId: UUID,
        questionTarget: RetrospectiveItemType?,
        inputTokens: Int,
        outputTokens: Int,
        action: ConversationTurnAction,
        answerGuideMode: ConversationAnswerGuideMode? = null,
        answerExamples: List<String> = emptyList(),
    ) {
        check(status == ConversationTurnStatus.PROCESSING)
        require(action == ConversationTurnAction.ASK || action == ConversationTurnAction.REFLECT || questionTarget == null) {
            "ASK 또는 선택형 REFLECT에만 질문 대상을 저장할 수 있습니다."
        }
        require((answerGuideMode == null) == answerExamples.isEmpty()) {
            "답변 예시 표시 방식과 예시는 함께 저장해야 합니다."
        }
        if (answerGuideMode != null) {
            require(action == ConversationTurnAction.ASK && questionTarget != null) {
                "ASK 턴에만 답변 예시를 저장할 수 있습니다."
            }
            require(answerExamples.size in 2..3) { "답변 예시는 두 개에서 세 개여야 합니다." }
            require(
                (questionTarget == RetrospectiveItemType.ACTION && answerGuideMode == ConversationAnswerGuideMode.INLINE) ||
                    (questionTarget != RetrospectiveItemType.ACTION && answerGuideMode == ConversationAnswerGuideMode.ON_DEMAND),
            ) { "질문 대상과 답변 예시 표시 방식이 일치하지 않습니다." }
        }
        this.assistantMessageId = assistantMessageId
        this.questionTarget = questionTarget
        this.action = action
        this.answerGuideMode = answerGuideMode
        this.answerExamples = answerExamples.toList()
        this.inputTokens += inputTokens
        this.outputTokens += outputTokens
        status = ConversationTurnStatus.COMPLETED
        completedAt = LocalDateTime.now()
    }

    fun fail(errorCode: String) {
        check(status == ConversationTurnStatus.PROCESSING)
        this.errorCode = errorCode
        status = ConversationTurnStatus.FAILED
        completedAt = LocalDateTime.now()
    }
}
