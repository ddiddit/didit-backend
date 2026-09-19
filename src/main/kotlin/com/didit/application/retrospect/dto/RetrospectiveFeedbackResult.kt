package com.didit.application.retrospect.dto

import com.didit.domain.retrospect.FeedbackRating
import com.didit.domain.retrospect.FeedbackReason
import com.didit.domain.retrospect.RetrospectiveFeedback
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveFeedbackResult(
    val id: UUID,
    val rating: FeedbackRating,
    val reasons: List<FeedbackReason>,
    val comment: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(feedback: RetrospectiveFeedback) =
            RetrospectiveFeedbackResult(
                feedback.id,
                feedback.rating,
                feedback.reasons,
                feedback.comment,
                feedback.createdAt,
                feedback.updatedAt,
            )
    }
}
