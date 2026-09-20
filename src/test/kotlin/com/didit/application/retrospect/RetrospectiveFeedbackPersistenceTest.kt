package com.didit.application.retrospect

import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.required.RetrospectiveFeedbackRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.FeedbackRating
import com.didit.domain.retrospect.FeedbackReason
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveFeedback
import com.didit.domain.retrospect.RetrospectiveResultV2
import com.didit.support.RepositoryTestSupport
import com.didit.support.RetrospectiveFixture
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Import(RetrospectiveFeedbackService::class)
class RetrospectiveFeedbackPersistenceTest : RepositoryTestSupport() {
    @Autowired lateinit var retrospectives: RetrospectiveRepository

    @Autowired lateinit var feedbacks: RetrospectiveFeedbackRepository

    @Autowired lateinit var service: RetrospectiveFeedbackService

    @Autowired lateinit var entityManager: EntityManager

    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `사유만 변경해도 응답과 저장된 최종 수정 시각이 갱신된다`() {
        val retrospective = retrospectives.save(RetrospectiveFixture.createCompleted())
        val first =
            service.submit(
                retrospective.id,
                retrospective.userId,
                FeedbackRating.HELPFUL,
                listOf(FeedbackReason.CORE_IDENTIFIED),
                null,
            )
        entityManager.flush()
        val oldTime = LocalDateTime.of(2000, 1, 1, 0, 0)
        entityManager
            .createNativeQuery("UPDATE retrospective_feedbacks SET updated_at = :time WHERE id = :id")
            .setParameter("time", oldTime)
            .setParameter("id", first.id)
            .executeUpdate()
        entityManager.clear()
        val updated =
            service.submit(
                retrospective.id,
                retrospective.userId,
                FeedbackRating.HELPFUL,
                listOf(FeedbackReason.WELL_ORGANIZED),
                null,
            )
        entityManager.flush()
        entityManager.clear()
        val stored = service.find(retrospective.id, retrospective.userId)
        assertThat(updated.updatedAt).isAfter(oldTime)
        assertThat(stored?.updatedAt).isCloseTo(updated.updatedAt, within(1, ChronoUnit.MICROS))
    }

    @Test
    fun `선택 사유 순서를 변경해도 정상 저장한다`() {
        val retrospective = retrospectives.save(RetrospectiveFixture.createCompleted())
        service.submit(
            retrospective.id,
            retrospective.userId,
            FeedbackRating.HELPFUL,
            listOf(FeedbackReason.CORE_IDENTIFIED, FeedbackReason.WELL_ORGANIZED),
            null,
        )
        entityManager.flush()
        entityManager.clear()
        service.submit(
            retrospective.id,
            retrospective.userId,
            FeedbackRating.HELPFUL,
            listOf(FeedbackReason.WELL_ORGANIZED, FeedbackReason.CORE_IDENTIFIED),
            null,
        )
        entityManager.flush()
        entityManager.clear()
        assertThat(service.find(retrospective.id, retrospective.userId)?.reasons)
            .containsExactly(FeedbackReason.WELL_ORGANIZED, FeedbackReason.CORE_IDENTIFIED)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `동시에 최초 제출해도 두 요청은 성공하고 평가 하나만 저장한다`() {
        val transaction = TransactionTemplate(transactionManager)
        val retrospective = requireNotNull(transaction.execute { retrospectives.save(RetrospectiveFixture.createCompleted()) })
        val executor = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        try {
            val tasks =
                (1..2).map {
                    executor.submit(
                        Callable {
                            ready.countDown()
                            check(start.await(20, TimeUnit.SECONDS))
                            service.submit(
                                retrospective.id,
                                retrospective.userId,
                                FeedbackRating.HELPFUL,
                                listOf(FeedbackReason.WELL_ORGANIZED),
                                null,
                            )
                        },
                    )
                }
            assertThat(ready.await(20, TimeUnit.SECONDS)).isTrue()
            start.countDown()
            val results = tasks.map { it.get(20, TimeUnit.SECONDS) }
            assertThat(results.map { it.id }.distinct()).hasSize(1)
            transaction.executeWithoutResult {
                val count =
                    entityManager
                        .createNativeQuery("SELECT COUNT(*) FROM retrospective_feedbacks WHERE retrospective_id = :id")
                        .setParameter("id", retrospective.id)
                        .singleResult as Number
                assertThat(count.toLong()).isEqualTo(1)
            }
        } finally {
            start.countDown()
            executor.shutdownNow()
            check(executor.awaitTermination(20, TimeUnit.SECONDS))
            transaction.executeWithoutResult {
                val feedback = feedbacks.findByRetrospectiveId(retrospective.id)
                if (feedback != null) entityManager.remove(feedback)
                retrospectives.findByIdAndUserId(retrospective.id, retrospective.userId)?.let(retrospectives::delete)
            }
        }
    }

    @Test
    fun `기존 및 V2 회고 모두 평가하고 반복 제출은 한 행만 유지한다`() {
        val userId = UUID.randomUUID()
        val legacy = RetrospectiveFixture.createCompleted(userId)
        val v2 =
            Retrospective.createV2(userId).apply {
                startProgress()
                finishConversation()
                saveV2Result("V2 회고", RetrospectiveResultV2(null, null, null, null, null, null, null))
            }
        listOf(legacy, v2).forEach { retrospective ->
            retrospectives.save(retrospective)
            val first = service.submit(retrospective.id, userId, FeedbackRating.HELPFUL, listOf(FeedbackReason.CUSTOM), "의견")
            entityManager.flush()
            entityManager.clear()
            val second =
                service.submit(
                    retrospective.id,
                    userId,
                    FeedbackRating.NEEDS_IMPROVEMENT,
                    listOf(FeedbackReason.MISSED_CONTEXT),
                    null,
                )
            entityManager.flush()
            entityManager.clear()
            val restored = service.find(retrospective.id, userId)
            assertThat(second.id).isEqualTo(first.id)
            assertThat(restored?.reasons).containsExactly(FeedbackReason.MISSED_CONTEXT)
            assertThat(restored?.comment).isNull()
            val count =
                entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM retrospective_feedbacks WHERE retrospective_id = :id")
                    .setParameter("id", retrospective.id)
                    .singleResult as Number
            assertThat(count.toLong()).isEqualTo(1)
        }
    }

    @Test
    fun `삭제된 회고의 평가는 보존하지만 사용자 접근은 차단한다`() {
        val retrospective = retrospectives.save(RetrospectiveFixture.createCompleted())
        service.submit(retrospective.id, retrospective.userId, FeedbackRating.HELPFUL, listOf(FeedbackReason.CUSTOM), "보존 의견")
        retrospective.softDelete()
        entityManager.flush()
        entityManager.clear()
        assertThrows<RetrospectiveNotFoundException> { service.find(retrospective.id, retrospective.userId) }
        assertThrows<RetrospectiveNotFoundException> {
            service.submit(retrospective.id, retrospective.userId, FeedbackRating.HELPFUL, emptyList(), null)
        }
        assertThat(feedbacks.findByRetrospectiveId(retrospective.id)?.comment).isEqualTo("보존 의견")
    }

    @Test
    fun `회고를 실제 삭제해도 피드백과 사유는 보존한다`() {
        val retrospective = retrospectives.save(RetrospectiveFixture.createCompleted())
        service.submit(retrospective.id, retrospective.userId, FeedbackRating.HELPFUL, listOf(FeedbackReason.CORE_IDENTIFIED), null)
        entityManager.flush()
        retrospectives.delete(retrospective)
        entityManager.flush()
        entityManager.clear()
        assertThat(feedbacks.findByRetrospectiveId(retrospective.id)?.reasons).containsExactly(FeedbackReason.CORE_IDENTIFIED)
    }

    @Test
    fun `DB도 회고당 평가 하나를 강제한다`() {
        val id = UUID.randomUUID()
        feedbacks.save(RetrospectiveFeedback.create(id, FeedbackRating.HELPFUL, emptyList(), null))
        entityManager.flush()
        feedbacks.save(RetrospectiveFeedback.create(id, FeedbackRating.HELPFUL, emptyList(), null))
        assertThrows<jakarta.persistence.PersistenceException> { entityManager.flush() }
    }
}
