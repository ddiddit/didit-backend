package com.didit.application.retrospect.required

import com.didit.domain.retrospect.Retrospective
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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
        val retro2 = retrospectiveRepository.save(Retrospective.create(userId).apply { softDelete() })

        val found = retrospectiveRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)

        assertThat(found).hasSize(1)
        assertThat(found[0].id).isEqualTo(retro1.id)
    }

    @Test
    fun `countByUserIdAndCreatedAtBetweenAndDeletedAtIsNull - 오늘 생성된 회고 수를 반환한다`() {
        retrospectiveRepository.save(Retrospective.create(userId))
        retrospectiveRepository.save(Retrospective.create(userId))

        val count =
            retrospectiveRepository.countByUserIdAndCreatedAtBetweenAndDeletedAtIsNull(
                userId = userId,
                from = LocalDateTime.now().toLocalDate().atStartOfDay(),
                to = LocalDateTime.now().toLocalDate().atTime(23, 59, 59),
            )

        assertThat(count).isEqualTo(2)
    }
}
