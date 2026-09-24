package com.didit.adapter.webapi.retrospect.dto

import com.didit.application.retrospect.dto.RetrospectiveFeedbackResult
import com.didit.domain.retrospect.FeedbackRating
import com.didit.domain.retrospect.FeedbackReason
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class RetrospectiveFeedbackRequest(
    val rating: FeedbackRating,
    @field:Size(max = 3)
    @field:JsonSetter(contentNulls = Nulls.FAIL)
    val reasons: List<FeedbackReason> = emptyList(),
    @field:Size(max = 500)
    val comment: String? = null,
)

data class RetrospectiveFeedbackResponse(
    val id: UUID,
    val rating: FeedbackRating,
    val reasons: List<FeedbackReason>,
    val comment: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(result: RetrospectiveFeedbackResult) =
            RetrospectiveFeedbackResponse(
                result.id,
                result.rating,
                result.reasons,
                result.comment,
                result.createdAt,
                result.updatedAt,
            )
    }
}
