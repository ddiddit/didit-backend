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
import java.time.Duration
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
    @Column(columnDefinition = "BINARY(16)")
    var projectId: UUID? = null,
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
    @Embedded
    var resultV2: RetrospectiveResultV2? = null,
    @Column
    var deletedAt: LocalDateTime? = null,
    @OneToMany(mappedBy = "retrospective", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val chatMessages: MutableList<ChatMessage> = mutableListOf(),
    @Column
    var completedAt: LocalDateTime? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var summaryGenerationStatus: SummaryGenerationStatus = SummaryGenerationStatus.NOT_STARTED,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val flowVersion: RetrospectiveFlowVersion = RetrospectiveFlowVersion.V1,
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var conversationStatus: ConversationStatus? = null,
    @Column
    var conversationFinishedAt: LocalDateTime? = null,
    @Column
    var resultGenerationStartedAt: LocalDateTime? = null,
    @Column(columnDefinition = "BINARY(16)")
    var resultGenerationAttemptId: UUID? = null,
) : BaseEntity() {
    fun isCompleted(): Boolean = status == RetroStatus.COMPLETED

    fun isInProgress(): Boolean = status == RetroStatus.IN_PROGRESS

    fun isDeleted(): Boolean = deletedAt != null

    fun isPending(): Boolean = status == RetroStatus.PENDING

    fun currentQuestionType(): QuestionType? = aiMessages().maxByOrNull { it.questionType.ordinal }?.questionType

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

    fun startProgress() {
        this.status = RetroStatus.IN_PROGRESS
    }

    fun isV2(): Boolean = flowVersion == RetrospectiveFlowVersion.V2

    fun isConversationActive(): Boolean = conversationStatus == ConversationStatus.ACTIVE

    fun finishConversation() {
        check(isV2()) { "V2 회고만 대화를 종료할 수 있습니다." }
        if (conversationStatus == ConversationStatus.FINISHED) return
        check(isConversationActive()) { "진행 중인 대화가 아닙니다." }
        conversationStatus = ConversationStatus.FINISHED
        conversationFinishedAt = LocalDateTime.now()
    }

    fun saveSummary(summary: RetrospectiveSummary) {
        this.summary = summary
        this.summaryGenerationStatus = SummaryGenerationStatus.GENERATED
    }

    fun saveV2Result(
        title: String,
        result: RetrospectiveResultV2,
    ) {
        check(isV2()) { "V2 회고만 구조화 결과를 저장할 수 있습니다." }
        this.resultV2 = result
        this.summaryGenerationStatus = SummaryGenerationStatus.GENERATED
        this.resultGenerationStartedAt = null
        this.resultGenerationAttemptId = null
        complete(title.trim())
    }

    fun startV2ResultGeneration(
        now: LocalDateTime = LocalDateTime.now(),
        attemptId: UUID = UUID.randomUUID(),
    ) {
        check(isV2()) { "V2 회고만 구조화 결과 생성을 시작할 수 있습니다." }
        check(summaryGenerationStatus != SummaryGenerationStatus.GENERATED) { "이미 구조화 결과를 생성했습니다." }
        summaryGenerationStatus = SummaryGenerationStatus.GENERATING
        resultGenerationStartedAt = now
        resultGenerationAttemptId = attemptId
    }

    fun isV2ResultGenerationStale(
        now: LocalDateTime,
        timeout: Duration,
    ): Boolean =
        summaryGenerationStatus == SummaryGenerationStatus.GENERATING &&
            (resultGenerationStartedAt == null || !resultGenerationStartedAt!!.isAfter(now.minus(timeout)))

    fun startSummaryGeneration() {
        check(summaryGenerationStatus == SummaryGenerationStatus.NOT_STARTED) { "이미 AI 요약을 생성 중이거나 생성했습니다." }
        summaryGenerationStatus = SummaryGenerationStatus.GENERATING
    }

    fun resetSummaryGeneration() {
        if (summaryGenerationStatus == SummaryGenerationStatus.GENERATING) {
            summaryGenerationStatus = SummaryGenerationStatus.NOT_STARTED
            resultGenerationStartedAt = null
            resultGenerationAttemptId = null
        }
    }

    fun addTokens(
        inputTokens: Int,
        outputTokens: Int,
    ) {
        this.inputTokens += inputTokens
        this.outputTokens += outputTokens
    }

    fun complete(title: String) {
        require(title.isNotBlank()) { "회고 제목은 비어 있을 수 없습니다." }
        require(title.length <= 25) { "회고 제목은 25자 이내여야 합니다." }

        this.title = title
        this.status = RetroStatus.COMPLETED
        this.completedAt = LocalDateTime.now()
    }

    fun updateTitle(newTitle: String) {
        require(newTitle.isNotBlank()) { "회고 제목은 비어 있을 수 없습니다." }
        require(newTitle.length <= 25) { "회고 제목은 25자 이내여야 합니다." }

        this.title = newTitle
    }

    fun softDelete() {
        this.deletedAt = LocalDateTime.now()
    }

    fun countDeepQuestionAnswers(): Int = validUserAnswers().count { it.questionType == QuestionType.Q4_DEEP }

    private fun aiMessages(): List<ChatMessage> = chatMessages.filter { it.sender == Sender.AI }

    private fun validUserAnswers(): List<ChatMessage> = chatMessages.filter { it.sender == Sender.USER && !it.isSkipped }

    companion object {
        fun create(userId: UUID): Retrospective = Retrospective(userId = userId)

        fun createV2(userId: UUID): Retrospective =
            Retrospective(
                userId = userId,
                flowVersion = RetrospectiveFlowVersion.V2,
                conversationStatus = ConversationStatus.ACTIVE,
            )
    }

    fun registerProject(projectId: UUID) {
        this.projectId = projectId
    }

    fun detachProject() {
        require(this.projectId != null) { "현재 회고에 할당된 프로젝트가 없습니다." }
        this.projectId = null
    }
}
