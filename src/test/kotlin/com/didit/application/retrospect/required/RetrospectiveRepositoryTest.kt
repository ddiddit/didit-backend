package com.didit.application.retrospect.required

import com.didit.application.organization.required.RetrospectTagRepository
import com.didit.domain.organization.RetrospectiveTag
import com.didit.domain.retrospect.RetroStatus
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveSummary
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.UUID

class RetrospectiveRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var retrospectiveRepository: RetrospectiveRepository

    @Autowired
    lateinit var retrospectiveTagRepository: RetrospectTagRepository

    private val userId = UUID.randomUUID()

    private fun summary(feedback: String = "피드백") =
        RetrospectiveSummary(
            summary = "...",
            blockedPoint = "...",
            solutionProcess = "...",
            lessonLearned = "...",
            insightTitle = "",
            insightDescription = "",
            nextActionTitle = "",
            nextActionDescription = "",
        )

    private fun completedRetrospective(
        userId: UUID,
        title: String,
        feedback: String = "피드백",
    ): Retrospective =
        Retrospective.create(userId).apply {
            startProgress()
            saveSummary(summary(feedback))
            complete(title = title)
        }

    @Test
    fun `save`() {
        val retro = Retrospective.create(userId)
        val saved = retrospectiveRepository.save(retro)
        assertThat(saved.userId).isEqualTo(userId)
    }

    @Test
    fun `findByIdAndUserId - 존재하는 경우`() {
        val retro = retrospectiveRepository.save(Retrospective.create(userId))
        val found = retrospectiveRepository.findByIdAndUserId(retro.id, userId)
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(retro.id)
    }

    @Test
    fun `findByIdAndUserId - 다른 유저이면 null을 반환한다`() {
        val retro = retrospectiveRepository.save(Retrospective.create(userId))
        val found = retrospectiveRepository.findByIdAndUserId(retro.id, UUID.randomUUID())
        assertThat(found).isNull()
    }

    @Test
    fun `findAllCompletedByUserId - COMPLETED 상태만 반환한다`() {
        retrospectiveRepository.save(Retrospective.create(userId))
        retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고"))

        val found = retrospectiveRepository.findAllCompletedByUserId(userId)

        assertThat(found).hasSize(1)
        assertThat(found[0].title).isEqualTo("완료된 회고")
    }

    @Test
    fun `findAllCompletedByUserId - 삭제된 회고는 제외된다`() {
        retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고"))
        retrospectiveRepository.save(completedRetrospective(userId, "삭제된 회고").apply { softDelete() })

        val found = retrospectiveRepository.findAllCompletedByUserId(userId)

        assertThat(found).hasSize(1)
        assertThat(found[0].title).isEqualTo("완료된 회고")
    }

    @Test
    fun `findRecentCompletedByUserId - limit만큼만 반환한다`() {
        repeat(5) { retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고 $it")) }

        val found =
            retrospectiveRepository.findRecentCompletedByUserId(
                userId = userId,
                pageable = PageRequest.of(0, 3),
            )

        assertThat(found).hasSize(3)
    }

    @Test
    fun `countByUserIdAndStatusNotAndCreatedAtBetween - PENDING 상태는 카운트에서 제외된다`() {
        retrospectiveRepository.save(Retrospective.create(userId))
        retrospectiveRepository.save(Retrospective.create(userId).apply { startProgress() })
        retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고"))

        val count =
            retrospectiveRepository.countByUserIdAndStatusNotAndCreatedAtBetween(
                userId = userId,
                status = RetroStatus.PENDING,
                from = LocalDateTime.now().toLocalDate().atStartOfDay(),
                to = LocalDateTime.now().toLocalDate().atTime(23, 59, 59),
            )

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `findCompletedByUserIdAndPeriod - COMPLETED 상태만 반환한다`() {
        retrospectiveRepository.save(Retrospective.create(userId))
        retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고"))

        val from =
            LocalDateTime
                .now()
                .toLocalDate()
                .withDayOfMonth(1)
                .atStartOfDay()
        val to = from.plusMonths(1)

        val found = retrospectiveRepository.findCompletedByUserIdAndPeriod(userId, from, to)

        assertThat(found).hasSize(1)
        assertThat(found[0].title).isEqualTo("완료된 회고")
    }

    @Test
    fun `searchByUserIdAndTitle`() {
        retrospectiveRepository.save(completedRetrospective(userId, "오늘 회고"))
        retrospectiveRepository.save(completedRetrospective(userId, "회고 정리"))
        retrospectiveRepository.save(completedRetrospective(UUID.randomUUID(), "오늘 회고"))
        retrospectiveRepository.save(completedRetrospective(userId, "회고 삭제됨").apply { softDelete() })

        val result = retrospectiveRepository.searchByUserIdAndTitle(userId, "회고")

        assertThat(result).hasSize(2)
    }

    @Test
    fun `countByUserIdAndStatusAndDeletedAtIsNull - COMPLETED 상태만 카운트한다`() {
        retrospectiveRepository.save(Retrospective.create(userId))
        retrospectiveRepository.save(Retrospective.create(userId).apply { startProgress() })
        retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고1"))
        retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고2"))
        retrospectiveRepository.save(completedRetrospective(userId, "삭제된 회고").apply { softDelete() })

        val count =
            retrospectiveRepository.countByUserIdAndStatusAndDeletedAtIsNull(
                userId = userId,
                status = RetroStatus.COMPLETED,
            )

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `findCompletedAtByUserIdAndStatusAndDeletedAtIsNull - 완료된 회고의 날짜를 반환한다`() {
        retrospectiveRepository.save(Retrospective.create(userId))
        retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고1"))
        retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고2"))
        retrospectiveRepository.save(completedRetrospective(userId, "삭제된 회고").apply { softDelete() })

        val dates =
            retrospectiveRepository.findCompletedAtByUserIdAndStatusAndDeletedAtIsNull(
                userId = userId,
                status = RetroStatus.COMPLETED,
            )

        assertThat(dates).hasSize(2)
    }

    @Test
    fun `findAllPendingBefore - PENDING 회고만 반환한다`() {
        retrospectiveRepository.save(Retrospective.create(userId))
        retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고"))

        val cutoff = LocalDateTime.now().plusDays(1)
        val result = retrospectiveRepository.findAllPendingBefore(cutoff)

        assertThat(result).hasSize(1)
        assertThat(result[0].status).isEqualTo(RetroStatus.PENDING)
    }

    @Test
    fun `findAllPendingBefore - softDelete된 PENDING 회고는 제외된다`() {
        retrospectiveRepository.save(Retrospective.create(userId))
        retrospectiveRepository.save(Retrospective.create(userId).apply { softDelete() })

        val cutoff = LocalDateTime.now().plusDays(1)
        val result = retrospectiveRepository.findAllPendingBefore(cutoff)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `findAllPendingBefore - cutoff 이후에 생성된 회고는 제외된다`() {
        retrospectiveRepository.save(Retrospective.create(userId))

        val cutoff = LocalDateTime.now().minusDays(1)
        val result = retrospectiveRepository.findAllPendingBefore(cutoff)

        assertThat(result).isEmpty()
    }

    @Test
    fun `findAllByTagId - 태그로 회고 목록을 조회한다`() {
        val userId = UUID.randomUUID()

        val retro1 =
            retrospectiveRepository.save(
                Retrospective.create(userId).apply {
                    startProgress()
                    complete("회고1")
                },
            )

        val retro2 =
            retrospectiveRepository.save(
                Retrospective.create(userId).apply {
                    startProgress()
                    complete("회고2")
                },
            )

        val retro3 =
            retrospectiveRepository.save(
                Retrospective.create(userId).apply {
                    startProgress()
                    complete("회고3")
                },
            )

        val tagId = UUID.randomUUID()

        val rt1 = RetrospectiveTag.add(retro1.id, tagId)
        val rt2 = RetrospectiveTag.add(retro2.id, tagId)

        val rtDeleted =
            RetrospectiveTag.add(retro3.id, tagId).apply {
                delete()
            }

        retrospectiveTagRepository.save(rt1)
        retrospectiveTagRepository.save(rt2)
        retrospectiveTagRepository.save(rtDeleted)

        val result = retrospectiveRepository.findAllByTagId(tagId)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.title })
            .containsExactlyInAnyOrder("회고1", "회고2")
    }
}
