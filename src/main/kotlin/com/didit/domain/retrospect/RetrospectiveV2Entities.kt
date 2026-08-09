package com.didit.domain.retrospect

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
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
) : BaseEntity() {
    fun update(
        newStatus: RetrospectiveItemStatus,
        newSummary: String,
    ) {
        if (newStatus.ordinal >= status.ordinal) status = newStatus
        summary = newSummary.trim().takeIf { it.isNotEmpty() }
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
    @Column(columnDefinition = "BINARY(16)")
    var assistantMessageId: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var questionTarget: RetrospectiveItemType? = null,
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
    ) {
        check(status == ConversationTurnStatus.PROCESSING)
        this.assistantMessageId = assistantMessageId
        this.questionTarget = questionTarget
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
