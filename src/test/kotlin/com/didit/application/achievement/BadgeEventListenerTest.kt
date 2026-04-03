package com.didit.application.achievement

import com.didit.application.achievement.provided.BadgeAwarder
import com.didit.domain.retrospect.RetrospectiveCompletedEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class BadgeEventListenerTest {
    @Mock
    lateinit var badgeAwarder: BadgeAwarder

    private lateinit var listener: BadgeEventListener

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        listener = BadgeEventListener(badgeAwarder)
    }

    @Test
    fun `onRetrospectiveCompleted - 이벤트 수신 시 배지 부여를 호출한다`() {
        val userId = UUID.randomUUID()
        val event = RetrospectiveCompletedEvent(userId = userId, retroDate = LocalDate.now())
        whenever(badgeAwarder.awardBadges(any(), any())).thenReturn(emptyList())

        listener.onRetrospectiveCompleted(event)

        verify(badgeAwarder).awardBadges(userId, event.retroDate)
    }

    @Test
    fun `onRetrospectiveCompleted - 배지 부여 실패해도 예외가 전파되지 않는다`() {
        val event = RetrospectiveCompletedEvent(userId = UUID.randomUUID(), retroDate = LocalDate.now())
        whenever(badgeAwarder.awardBadges(any(), any())).thenThrow(RuntimeException("배지 부여 실패"))

        org.junit.jupiter.api.assertDoesNotThrow {
            listener.onRetrospectiveCompleted(event)
        }
        verify(badgeAwarder).awardBadges(any(), any())
    }
}
