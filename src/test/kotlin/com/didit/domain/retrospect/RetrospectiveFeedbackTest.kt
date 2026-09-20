package com.didit.domain.retrospect

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class RetrospectiveFeedbackTest {
    private val retrospectiveId = UUID.randomUUID()

    @Test
    fun `사유 없이 평가만 제출할 수 있다`() {
        val feedback = RetrospectiveFeedback.create(retrospectiveId, FeedbackRating.HELPFUL, emptyList(), null)
        assertThat(feedback.rating).isEqualTo(FeedbackRating.HELPFUL)
        assertThat(feedback.reasons).isEmpty()
        assertThat(feedback.comment).isNull()
    }

    @Test
    fun `직접 입력 포함 세 사유와 500자 의견을 저장한다`() {
        val feedback =
            RetrospectiveFeedback.create(
                retrospectiveId,
                FeedbackRating.HELPFUL,
                listOf(FeedbackReason.CORE_IDENTIFIED, FeedbackReason.WELL_ORGANIZED, FeedbackReason.CUSTOM),
                "가".repeat(500),
            )
        assertThat(feedback.reasons).containsExactly(FeedbackReason.CORE_IDENTIFIED, FeedbackReason.WELL_ORGANIZED, FeedbackReason.CUSTOM)
        assertThat(feedback.comment).hasSize(500)
    }

    @Test
    fun `네 사유 선택은 거부한다`() {
        assertThrows<IllegalArgumentException> {
            RetrospectiveFeedback.create(
                retrospectiveId,
                FeedbackRating.HELPFUL,
                listOf(FeedbackReason.CORE_IDENTIFIED, FeedbackReason.DEEP_QUESTIONS, FeedbackReason.WELL_ORGANIZED, FeedbackReason.CUSTOM),
                "의견",
            )
        }
    }

    @Test
    fun `중복 사유와 반대 평가의 사유는 거부한다`() {
        val invalidReasons =
            listOf(
                listOf(FeedbackReason.CORE_IDENTIFIED, FeedbackReason.CORE_IDENTIFIED),
                listOf(FeedbackReason.MISSED_CONTEXT),
            )
        invalidReasons.forEach { reasons ->
            assertThrows<IllegalArgumentException> { RetrospectiveFeedback.create(retrospectiveId, FeedbackRating.HELPFUL, reasons, null) }
        }
    }

    @Test
    fun `직접 입력은 비어 있거나 500자를 넘으면 거부한다`() {
        listOf(null, "", "   ", "가".repeat(501)).forEach { comment ->
            assertThrows<IllegalArgumentException> {
                RetrospectiveFeedback.create(retrospectiveId, FeedbackRating.NEEDS_IMPROVEMENT, listOf(FeedbackReason.CUSTOM), comment)
            }
        }
    }

    @Test
    fun `직접 입력 선택 없이 의견을 보내면 거부한다`() {
        assertThrows<IllegalArgumentException> {
            RetrospectiveFeedback.create(retrospectiveId, FeedbackRating.HELPFUL, emptyList(), "의견")
        }
    }

    @Test
    fun `평가 변경은 같은 ID를 유지하며 사유와 의견을 교체한다`() {
        val feedback = RetrospectiveFeedback.create(retrospectiveId, FeedbackRating.HELPFUL, listOf(FeedbackReason.CUSTOM), "기존 의견")
        val id = feedback.id
        feedback.update(FeedbackRating.NEEDS_IMPROVEMENT, listOf(FeedbackReason.INACCURATE_CONTENT), null)
        assertThat(feedback.id).isEqualTo(id)
        assertThat(feedback.rating).isEqualTo(FeedbackRating.NEEDS_IMPROVEMENT)
        assertThat(feedback.reasons).containsExactly(FeedbackReason.INACCURATE_CONTENT)
        assertThat(feedback.comment).isNull()
    }

    @Test
    fun `잘못된 변경은 기존 평가를 유지한다`() {
        val feedback = RetrospectiveFeedback.create(retrospectiveId, FeedbackRating.HELPFUL, listOf(FeedbackReason.CORE_IDENTIFIED), null)
        assertThrows<IllegalArgumentException> {
            feedback.update(
                FeedbackRating.NEEDS_IMPROVEMENT,
                listOf(FeedbackReason.CORE_IDENTIFIED),
                null,
            )
        }
        assertThat(feedback.rating).isEqualTo(FeedbackRating.HELPFUL)
        assertThat(feedback.reasons).containsExactly(FeedbackReason.CORE_IDENTIFIED)
    }
}
