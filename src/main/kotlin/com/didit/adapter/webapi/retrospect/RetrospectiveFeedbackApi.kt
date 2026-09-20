package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.adapter.webapi.retrospect.dto.RetrospectiveFeedbackRequest
import com.didit.adapter.webapi.retrospect.dto.RetrospectiveFeedbackResponse
import com.didit.application.retrospect.provided.RetrospectiveFeedbackFinder
import com.didit.application.retrospect.provided.RetrospectiveFeedbackSubmitter
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class RetrospectiveFeedbackApi(
    private val feedbackSubmitter: RetrospectiveFeedbackSubmitter,
    private val feedbackFinder: RetrospectiveFeedbackFinder,
) {
    @RequireAuth
    @PutMapping("/api/v2/retrospectives/{retrospectiveId}/feedback")
    fun submit(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @Valid @RequestBody request: RetrospectiveFeedbackRequest,
    ): SuccessResponse<RetrospectiveFeedbackResponse> =
        SuccessResponse.of(
            RetrospectiveFeedbackResponse.from(
                feedbackSubmitter.submit(retrospectiveId, userId, request.rating, request.reasons, request.comment),
            ),
        )

    @RequireAuth
    @GetMapping("/api/v2/retrospectives/{retrospectiveId}/feedback")
    fun find(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
    ): SuccessResponse<RetrospectiveFeedbackResponse?> =
        SuccessResponse.of(feedbackFinder.find(retrospectiveId, userId)?.let(RetrospectiveFeedbackResponse::from))
}
