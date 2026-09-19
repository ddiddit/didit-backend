package com.didit.domain.retrospect

import com.didit.domain.shared.BaseEntity
import com.fasterxml.jackson.annotation.JsonCreator
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OrderColumn
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

enum class FeedbackRating {
    HELPFUL,
    NEEDS_IMPROVEMENT,
    ;

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(value: String): FeedbackRating = entries.firstOrNull { it.name == value } ?: throw IllegalArgumentException()
    }
}

enum class FeedbackReason(
    val rating: FeedbackRating?,
) {
    CORE_IDENTIFIED(FeedbackRating.HELPFUL),
    DEEP_QUESTIONS(FeedbackRating.HELPFUL),
    WELL_ORGANIZED(FeedbackRating.HELPFUL),
    SIMPLE_QUESTIONS(FeedbackRating.NEEDS_IMPROVEMENT),
    INACCURATE_CONTENT(FeedbackRating.NEEDS_IMPROVEMENT),
    MISSED_CONTEXT(FeedbackRating.NEEDS_IMPROVEMENT),
    CUSTOM(null),
    ;

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(value: String): FeedbackReason = entries.firstOrNull { it.name == value } ?: throw IllegalArgumentException()
    }
}

@Entity
@Table(name = "retrospective_feedbacks", uniqueConstraints = [UniqueConstraint(columnNames = ["retrospective_id"])])
class RetrospectiveFeedback private constructor(
    @Column(columnDefinition = "BINARY(16)", nullable = false)
    val retrospectiveId: UUID,
    rating: FeedbackRating,
    reasons: List<FeedbackReason>,
    comment: String?,
) : BaseEntity() {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID()

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var rating: FeedbackRating = rating
        protected set

    @ElementCollection
    @CollectionTable(
        name = "retrospective_feedback_reasons",
        joinColumns = [JoinColumn(name = "feedback_id")],
    )
    @OrderColumn(name = "reason_order")
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 32)
    private var selectedReasons: MutableList<FeedbackReason> = reasons.toMutableList()

    val reasons: List<FeedbackReason>
        get() = selectedReasons.toList()

    @Column(length = 500)
    var comment: String? = comment
        protected set

    fun update(
        rating: FeedbackRating,
        reasons: List<FeedbackReason>,
        comment: String?,
    ) {
        validate(rating, reasons, comment)
        // 동일 요청은 collection을 다시 저장하거나 수정 시각을 변경하지 않는다.
        if (this.rating == rating && this.reasons == reasons && this.comment == comment?.trim()) return
        this.rating = rating
        selectedReasons.clear()
        selectedReasons.addAll(reasons)
        this.comment = comment?.trim()
        // 사유 collection만 바뀌어도 owner의 auditing이 실행되도록 변경을 표시한다.
        updatedAt = LocalDateTime.now()
    }

    companion object {
        fun create(
            retrospectiveId: UUID,
            rating: FeedbackRating,
            reasons: List<FeedbackReason>,
            comment: String?,
        ): RetrospectiveFeedback {
            validate(rating, reasons, comment)
            return RetrospectiveFeedback(retrospectiveId, rating, reasons, comment?.trim())
        }

        private fun validate(
            rating: FeedbackRating,
            reasons: List<FeedbackReason>,
            comment: String?,
        ) {
            require(reasons.size <= 3) { "사유는 직접 입력 포함 최대 3개까지 선택할 수 있습니다." }
            require(reasons.distinct().size == reasons.size) { "사유를 중복 선택할 수 없습니다." }
            require(reasons.all { it.rating == null || it.rating == rating }) { "평가에 맞는 사유를 선택해주세요." }
            if (FeedbackReason.CUSTOM in reasons) {
                require(!comment.isNullOrBlank()) { "직접 입력 의견을 작성해주세요." }
                require(comment.length <= 500) { "직접 입력은 최대 500자입니다." }
            } else {
                require(comment == null) { "직접 입력 사유를 선택해주세요." }
            }
        }
    }
}
