package com.didit.application.retrospect

import com.didit.application.retrospect.exception.InvalidRetrospectiveFeedbackException
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.exception.SummaryNotGeneratedException
import com.didit.application.retrospect.required.RetrospectiveFeedbackRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.FeedbackRating
import com.didit.domain.retrospect.FeedbackReason
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveFeedback
import com.didit.support.RetrospectiveFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class RetrospectiveFeedbackServiceTest {
    private val retrospectives = mock<RetrospectiveRepository>()
    private val feedbacks = mock<RetrospectiveFeedbackRepository>()
    private val service = RetrospectiveFeedbackService(retrospectives, feedbacks)
    private val userId = UUID.randomUUID()
    private val completed = RetrospectiveFixture.createCompleted(userId)
    private val id = completed.id

    @Test
    fun `평가를 최초 등록하고 동일 요청은 기존 평가를 반환한다`() {
        whenever(retrospectives.findByIdAndUserIdAndDeletedAtIsNullForUpdate(id, userId)).thenReturn(completed)
        whenever(feedbacks.save(any())).thenAnswer { it.arguments[0] }
        val first = service.submit(id, userId, FeedbackRating.HELPFUL, emptyList(), null)
        val stored = RetrospectiveFeedback.create(id, FeedbackRating.HELPFUL, emptyList(), null)
        whenever(feedbacks.findByRetrospectiveId(id)).thenReturn(stored)
        val repeated = service.submit(id, userId, FeedbackRating.HELPFUL, emptyList(), null)
        assertThat(first.rating).isEqualTo(FeedbackRating.HELPFUL)
        assertThat(repeated.id).isEqualTo(stored.id)
    }

    @Test
    fun `평가 변경은 기존 행의 사유와 의견을 교체한다`() {
        val stored = RetrospectiveFeedback.create(id, FeedbackRating.HELPFUL, listOf(FeedbackReason.CUSTOM), "기존 의견")
        whenever(retrospectives.findByIdAndUserIdAndDeletedAtIsNullForUpdate(id, userId)).thenReturn(completed)
        whenever(feedbacks.findByRetrospectiveId(id)).thenReturn(stored)
        val result = service.submit(id, userId, FeedbackRating.NEEDS_IMPROVEMENT, listOf(FeedbackReason.MISSED_CONTEXT), null)
        assertThat(result.id).isEqualTo(stored.id)
        assertThat(result.reasons).containsExactly(FeedbackReason.MISSED_CONTEXT)
        assertThat(result.comment).isNull()
        verify(feedbacks, never()).save(any())
    }

    @Test
    fun `소유권 또는 삭제 조건을 만족하지 않으면 등록과 조회를 거부한다`() {
        assertThrows<RetrospectiveNotFoundException> { service.submit(id, userId, FeedbackRating.HELPFUL, emptyList(), null) }
        assertThrows<RetrospectiveNotFoundException> { service.find(id, userId) }
        verify(feedbacks, never()).save(any())
        verify(feedbacks, never()).findByRetrospectiveId(any())
    }

    @Test
    fun `미완료 회고와 결과가 생성되지 않은 회고는 평가할 수 없다`() {
        val inProgress = Retrospective.create(userId)
        val noResult =
            Retrospective.create(userId).apply {
                startProgress()
                complete("결과 없음")
            }
        listOf(inProgress, noResult).forEach { retrospective ->
            whenever(retrospectives.findByIdAndUserIdAndDeletedAtIsNullForUpdate(id, userId)).thenReturn(retrospective)
            whenever(retrospectives.findByIdAndUserIdAndDeletedAtIsNull(id, userId)).thenReturn(retrospective)
            assertThrows<SummaryNotGeneratedException> { service.submit(id, userId, FeedbackRating.HELPFUL, emptyList(), null) }
            assertThrows<SummaryNotGeneratedException> { service.find(id, userId) }
        }
    }

    @Test
    fun `잘못된 사유는 비즈니스 오류로 반환하고 저장하지 않는다`() {
        whenever(retrospectives.findByIdAndUserIdAndDeletedAtIsNullForUpdate(id, userId)).thenReturn(completed)
        assertThrows<InvalidRetrospectiveFeedbackException> {
            service.submit(id, userId, FeedbackRating.HELPFUL, listOf(FeedbackReason.SIMPLE_QUESTIONS), null)
        }
        verify(feedbacks, never()).save(any())
    }

    @Test
    fun `미제출 평가 조회는 null이고 제출한 평가는 복원한다`() {
        whenever(retrospectives.findByIdAndUserIdAndDeletedAtIsNull(id, userId)).thenReturn(completed)
        assertThat(service.find(id, userId)).isNull()
        val stored = RetrospectiveFeedback.create(id, FeedbackRating.HELPFUL, listOf(FeedbackReason.CUSTOM), "새 질문이 좋았어요")
        whenever(feedbacks.findByRetrospectiveId(id)).thenReturn(stored)
        assertThat(service.find(id, userId)?.comment).isEqualTo("새 질문이 좋았어요")
    }
}
