package com.didit.application.retrospect.provided

import com.didit.application.retrospect.dto.RetrospectiveFeedbackResult
import com.didit.domain.retrospect.FeedbackRating
import com.didit.domain.retrospect.FeedbackReason
import java.util.UUID

interface RetrospectiveFeedbackSubmitter {
    fun submit(
        retrospectiveId: UUID,
        userId: UUID,
        rating: FeedbackRating,
        reasons: List<FeedbackReason>,
        comment: String?,
    ): RetrospectiveFeedbackResult
}

interface RetrospectiveFeedbackFinder {
    fun find(
        retrospectiveId: UUID,
        userId: UUID,
    ): RetrospectiveFeedbackResult?
}
