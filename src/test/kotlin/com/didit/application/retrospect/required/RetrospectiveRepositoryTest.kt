package com.didit.application.retrospect.required

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

    private val userId = UUID.randomUUID()

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
    fun `findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc - 삭제된 회고는 제외된다`() {
        val retro1 = retrospectiveRepository.save(Retrospective.create(userId))
        retrospectiveRepository.save(Retrospective.create(userId).apply { softDelete() })

        val found = retrospectiveRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)

        assertThat(found).hasSize(1)
        assertThat(found[0].id).isEqualTo(retro1.id)
    }

    @Test
    fun `countByUserIdAndStatusNotAndCreatedAtBetween - PENDING 상태는 카운트에서 제외된다`() {
        retrospectiveRepository.save(Retrospective.create(userId))
        retrospectiveRepository.save(Retrospective.create(userId).apply { startProgress() })
        retrospectiveRepository.save(
            Retrospective.create(userId).apply {
                startProgress()
                complete(
                    title = "완료된 회고",
                    summary =
                        RetrospectiveSummary(
                            feedback = "",
                            insight = "",
                            doneWork = "",
                            blockedPoint = "",
                            solutionProcess = "",
                            lessonLearned = "",
                        ),
                    inputTokens = 0,
                    outputTokens = 0,
                )
            },
        )

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
    fun `findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc - limit만큼만 반환한다`() {
        repeat(5) { retrospectiveRepository.save(Retrospective.create(userId)) }

        val found =
            retrospectiveRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                userId = userId,
                pageable = PageRequest.of(0, 5),
            )

        assertThat(found).hasSize(5)
    }

    @Test
    fun `findFirstByUserIdAndStatusAndDeletedAtIsNull - 가장 최근 완료된 회고를 반환한다`() {
        retrospectiveRepository.save(Retrospective.create(userId)) // PENDING
        retrospectiveRepository.save(
            Retrospective.create(userId).apply {
                startProgress()
                complete(
                    title = "완료된 회고",
                    summary =
                        RetrospectiveSummary(
                            feedback = "피드백",
                            insight = "",
                            doneWork = "",
                            blockedPoint = "",
                            solutionProcess = "",
                            lessonLearned = "",
                        ),
                    inputTokens = 0,
                    outputTokens = 0,
                )
            },
        )

        val found =
            retrospectiveRepository.findFirstByUserIdAndStatusAndDeletedAtIsNull(
                userId = userId,
                status = RetroStatus.COMPLETED,
            )

        assertThat(found).isNotNull
        assertThat(found!!.title).isEqualTo("완료된 회고")
    }

    @Test
    fun `searchByUserIdAndTitle`() {
        retrospectiveRepository.save(
            Retrospective.create(userId).apply {
                updateTitle("오늘 회고")
            },
        )

        retrospectiveRepository.save(
            Retrospective.create(userId).apply {
                updateTitle("회고 정리")
            },
        )
        retrospectiveRepository.save(
            Retrospective.create(UUID.randomUUID()).apply {
                updateTitle("오늘 회고")
            },
        )
        retrospectiveRepository.save(
            Retrospective.create(userId).apply {
                updateTitle("회고 삭제됨")
                softDelete()
            },
        )
        val result = retrospectiveRepository.searchByUserIdAndTitle(userId, "회고")
        assertThat(result).hasSize(2)
        assertThat(result.map { it.title }).containsExactly("회고 정리", "오늘 회고")
    }
}
