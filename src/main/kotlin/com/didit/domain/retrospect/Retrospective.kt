package com.didit.domain.retrospect

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
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
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(nullable = true, length = 255)
    var title: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: RetroStatus = RetroStatus.PENDING,
    @Column(nullable = false)
    var inputTokens: Int = 0,
    @Column(nullable = false)
    var outputTokens: Int = 0,
    @Embedded
    var summary: RetrospectiveSummary? = null,
    @Column
    var deletedAt: LocalDateTime? = null,
    @OneToMany(mappedBy = "retrospective", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val chatMessages: MutableList<ChatMessage> = mutableListOf(),
    @Column
    var completedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun isCompleted(): Boolean = status == RetroStatus.COMPLETED

    fun isInProgress(): Boolean = status == RetroStatus.IN_PROGRESS

    fun isDeleted(): Boolean = deletedAt != null

    fun currentQuestionType(): QuestionType? =
        aiMessages()
            .maxByOrNull { it.questionType.ordinal }
            ?.questionType

    fun hasDeepQuestion(): Boolean = aiMessages().any { it.questionType == QuestionType.Q4_DEEP }

    fun canAddDeepQuestion(): Boolean {
        val answeredTypes = validUserAnswers().map { it.questionType }.toSet()
        val hasAllBaseAnswers =
            answeredTypes.containsAll(
                listOf(QuestionType.Q1, QuestionType.Q2, QuestionType.Q3),
            )
        return hasAllBaseAnswers && !hasDeepQuestion()
    }

    fun getAnswersUpToQ3(): List<String> =
        validUserAnswers()
            .filter { it.questionType != QuestionType.Q4_DEEP }
            .sortedBy { it.questionType.ordinal }
            .map { it.content }

    fun getAllAnswers(): List<String> =
        validUserAnswers()
            .sortedBy { it.questionType.ordinal }
            .map { it.content }

    fun addMessage(message: ChatMessage) {
        chatMessages.add(message)
    }

    fun isPending(): Boolean = status == RetroStatus.PENDING

    fun startProgress() {
        this.status = RetroStatus.IN_PROGRESS
    }

    fun complete(
        title: String,
        summary: RetrospectiveSummary,
        inputTokens: Int,
        outputTokens: Int,
    ) {
        require(title.isNotBlank()) { "회고 제목은 비어 있을 수 없습니다." }

        this.title = title
        this.summary = summary
        this.inputTokens = inputTokens
        this.outputTokens = outputTokens
        this.status = RetroStatus.COMPLETED
        this.completedAt = LocalDateTime.now()
    }

    fun updateTitle(newTitle: String) {
        require(newTitle.isNotBlank()) { "회고 제목은 비어 있을 수 없습니다." }

        this.title = newTitle
    }

    fun softDelete() {
        this.deletedAt = LocalDateTime.now()
    }

    private fun aiMessages(): List<ChatMessage> = chatMessages.filter { it.sender == Sender.AI }

    private fun validUserAnswers(): List<ChatMessage> = chatMessages.filter { it.sender == Sender.USER && !it.isSkipped }

    companion object {
        fun create(userId: UUID): Retrospective = Retrospective(userId = userId)
    }
}
