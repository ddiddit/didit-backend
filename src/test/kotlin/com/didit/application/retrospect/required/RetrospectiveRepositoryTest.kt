package com.didit.application.retrospect.required

import com.didit.application.organization.required.RetrospectTagRepository
import com.didit.domain.organization.RetrospectiveTag
import com.didit.domain.retrospect.RetroStatus
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveResultDetail
import com.didit.domain.retrospect.RetrospectiveResultV2
import com.didit.domain.retrospect.RetrospectiveSummary
import com.didit.support.RepositoryTestSupport
import jakarta.persistence.EntityManager
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

    @Autowired
    lateinit var entityManager: EntityManager

    private val userId = UUID.randomUUID()

    private fun summary(feedback: String = "피드백") =
        RetrospectiveSummary(
            summary = "...",
            blockedPoint = listOf("..."),
            solutionProcess = listOf("..."),
            lessonLearned = listOf("..."),
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
    fun `findListItemsByUserId - 응답 필드만 조회하고 완료된 회고만 반환한다`() {
        retrospectiveRepository.save(Retrospective.create(userId))
        retrospectiveRepository.save(completedRetrospective(userId, "삭제된 회고").apply { softDelete() })
        val completed = retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고"))
        retrospectiveRepository.save(completedRetrospective(UUID.randomUUID(), "다른 사용자 회고"))

        val found = retrospectiveRepository.findListItemsByUserId(userId)

        assertThat(found).hasSize(1)
        assertThat(found.single().id).isEqualTo(completed.id)
        assertThat(found.single().title).isEqualTo("완료된 회고")
        assertThat(found.single().summary).isEqualTo("...")
        assertThat(found.single().completedAt).isNotNull()
    }

    @Test
    fun `V2 구조화 결과를 저장하고 목록 요약으로 조회한다`() {
        val retrospective =
            Retrospective.createV2(userId).apply {
                startProgress()
                finishConversation()
                saveV2Result(
                    title = "배포 장애 대응 회고",
                    result =
                        RetrospectiveResultV2(
                            summary = "배포 직후 장애를 발견하고 롤백했다.",
                            strengths = null,
                            improvements = listOf("배포 전 확인이 부족했다."),
                            processes = listOf("로그를 확인해 원인을 좁혔다."),
                            learnings = listOf("체크리스트가 필요하다."),
                            insight = null,
                            nextActions =
                                listOf(
                                    RetrospectiveResultDetail("배포 체크리스트 작성", "배포 전 확인 항목을 정리한다."),
                                    RetrospectiveResultDetail("알림 기준 점검", "장애 알림 임계값을 확인한다."),
                                ),
                        ),
                )
            }
        retrospectiveRepository.save(retrospective)
        entityManager.flush()
        entityManager.clear()

        val saved = retrospectiveRepository.findByIdAndUserId(retrospective.id, userId)!!
        val listItem = retrospectiveRepository.findListItemsByUserId(userId).single()

        assertThat(saved.resultV2?.strengths).isNull()
        assertThat(saved.resultV2?.nextActions)
            .containsExactly(
                RetrospectiveResultDetail("배포 체크리스트 작성", "배포 전 확인 항목을 정리한다."),
                RetrospectiveResultDetail("알림 기준 점검", "장애 알림 임계값을 확인한다."),
            )
        assertThat(listItem.summary).isEqualTo("배포 직후 장애를 발견하고 롤백했다.")
    }

    @Test
    fun `V2 결과 항목이 모두 비어 있어도 생성된 결과 객체를 보존한다`() {
        val retrospective =
            Retrospective.createV2(userId).apply {
                startProgress()
                finishConversation()
                saveV2Result(
                    title = "확인된 내용이 적은 회고",
                    result = RetrospectiveResultV2(null, null, null, null, null, null, null),
                )
            }
        retrospectiveRepository.save(retrospective)
        entityManager.flush()
        entityManager.clear()

        val saved = retrospectiveRepository.findByIdAndUserId(retrospective.id, userId)!!

        assertThat(saved.resultV2).isNotNull
        assertThat(saved.resultV2?.schemaVersion).isEqualTo(3)
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
    fun `countByUserIdAndCreatedAtInPeriod - PENDING 상태는 카운트에서 제외된다`() {
        retrospectiveRepository.save(Retrospective.create(userId))
        retrospectiveRepository.save(Retrospective.create(userId).apply { startProgress() })
        retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고"))

        val count =
            retrospectiveRepository.countByUserIdAndCreatedAtInPeriod(
                userId = userId,
                status = RetroStatus.PENDING,
                from = LocalDateTime.now().toLocalDate().atStartOfDay(),
                to =
                    LocalDateTime
                        .now()
                        .toLocalDate()
                        .plusDays(1)
                        .atStartOfDay(),
            )

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `countByUserIdAndCreatedAtInPeriod - 삭제된 회고는 카운트에서 제외된다`() {
        retrospectiveRepository.save(Retrospective.create(userId).apply { startProgress() })
        retrospectiveRepository.save(completedRetrospective(userId, "완료된 회고"))
        retrospectiveRepository.save(completedRetrospective(userId, "삭제된 회고").apply { softDelete() })

        val count =
            retrospectiveRepository.countByUserIdAndCreatedAtInPeriod(
                userId = userId,
                status = RetroStatus.PENDING,
                from = LocalDateTime.now().toLocalDate().atStartOfDay(),
                to =
                    LocalDateTime
                        .now()
                        .toLocalDate()
                        .plusDays(1)
                        .atStartOfDay(),
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
