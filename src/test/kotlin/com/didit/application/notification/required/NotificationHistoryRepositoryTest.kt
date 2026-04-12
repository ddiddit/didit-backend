package com.didit.application.notification.required

import com.didit.domain.notification.NotificationHistory
import com.didit.support.NotificationHistoryFixture
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class NotificationHistoryRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var notificationHistoryRepository: NotificationHistoryRepository

    @Test
    fun `save`() {
        val request = NotificationHistoryFixture.createRequest()
        val history = NotificationHistory.create(request)

        val saved = notificationHistoryRepository.save(history)

        assertThat(saved.userId).isEqualTo(history.userId)
        assertThat(saved.isRead).isFalse()
    }

    @Test
    fun `findAllByUserIdAndCreatedAtAfterOrderByCreatedAtDesc`() {
        val userId = UUID.randomUUID()
        notificationHistoryRepository.save(NotificationHistory.create(NotificationHistoryFixture.createRequest(userId)))

        val found =
            notificationHistoryRepository
                .findAllByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
                    userId = userId,
                    createdAt = LocalDateTime.now().minusDays(30),
                )

        assertThat(found).hasSize(1)
        assertThat(found[0].userId).isEqualTo(userId)
    }

    @Test
    fun `deleteAllByUserId - 해당 유저의 알림 히스토리가 삭제된다`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()

        notificationHistoryRepository.save(NotificationHistory.create(NotificationHistoryFixture.createRequest(userId)))
        notificationHistoryRepository.save(NotificationHistory.create(NotificationHistoryFixture.createRequest(userId)))
        notificationHistoryRepository.save(NotificationHistory.create(NotificationHistoryFixture.createRequest(otherUserId)))

        notificationHistoryRepository.deleteAllByUserId(userId)

        val remaining =
            notificationHistoryRepository
                .findAllByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, LocalDateTime.now().minusDays(30))
        assertThat(remaining).isEmpty()

        val otherRemaining =
            notificationHistoryRepository
                .findAllByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(otherUserId, LocalDateTime.now().minusDays(30))
        assertThat(otherRemaining).hasSize(1)
    }
}
