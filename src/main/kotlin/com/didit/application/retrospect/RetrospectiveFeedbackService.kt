package com.didit.application.retrospect

import com.didit.application.retrospect.dto.RetrospectiveFeedbackResult
import com.didit.application.retrospect.exception.InvalidRetrospectiveFeedbackException
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.exception.SummaryNotGeneratedException
import com.didit.application.retrospect.provided.RetrospectiveFeedbackFinder
import com.didit.application.retrospect.provided.RetrospectiveFeedbackSubmitter
import com.didit.application.retrospect.required.RetrospectiveFeedbackRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.FeedbackRating
import com.didit.domain.retrospect.FeedbackReason
import com.didit.domain.retrospect.RetrospectiveFeedback
import com.didit.domain.retrospect.SummaryGenerationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class RetrospectiveFeedbackService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val feedbackRepository: RetrospectiveFeedbackRepository,
) : RetrospectiveFeedbackSubmitter,
    RetrospectiveFeedbackFinder {
    @Transactional
    override fun submit(
        retrospectiveId: UUID,
        userId: UUID,
        rating: FeedbackRating,
        reasons: List<FeedbackReason>,
        comment: String?,
    ): RetrospectiveFeedbackResult {
        findCompleted(retrospectiveId, userId, forUpdate = true)
        val existing = feedbackRepository.findByRetrospectiveId(retrospectiveId)
        val feedback =
            try {
                if (existing == null) {
                    feedbackRepository.save(RetrospectiveFeedback.create(retrospectiveId, rating, reasons, comment))
                } else {
                    existing.apply { update(rating, reasons, comment) }
                }
            } catch (_: IllegalArgumentException) {
                throw InvalidRetrospectiveFeedbackException()
            }
        feedbackRepository.flush()
        return RetrospectiveFeedbackResult.from(feedback)
    }

    override fun find(
        retrospectiveId: UUID,
        userId: UUID,
    ): RetrospectiveFeedbackResult? {
        findCompleted(retrospectiveId, userId)
        return feedbackRepository.findByRetrospectiveId(retrospectiveId)?.let(RetrospectiveFeedbackResult::from)
    }

    private fun findCompleted(
        retrospectiveId: UUID,
        userId: UUID,
        forUpdate: Boolean = false,
    ) {
        val retrospective =
            if (forUpdate) {
                retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNullForUpdate(retrospectiveId, userId)
            } else {
                retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNull(retrospectiveId, userId)
            } ?: throw RetrospectiveNotFoundException(retrospectiveId)
        if (!retrospective.isCompleted() || retrospective.summaryGenerationStatus != SummaryGenerationStatus.GENERATED) {
            throw SummaryNotGeneratedException(retrospectiveId)
        }
    }
}
