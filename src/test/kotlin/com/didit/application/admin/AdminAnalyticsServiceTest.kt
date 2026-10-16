package com.didit.application.admin

import com.didit.application.admin.required.AdminAnalyticsRepository
import com.didit.application.admin.required.AdminAnalyticsSnapshot
import com.didit.application.admin.required.AnalyticsRetrospective
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class AdminAnalyticsServiceTest {
    @Mock
    lateinit var repository: AdminAnalyticsRepository

    @Test
    fun `고객 회고 스냅샷으로 공식 KPI를 계산한다`() {
        whenever(repository.load(any(), any(), any(), any())).thenReturn(
            AdminAnalyticsSnapshot(
                retrospectives =
                    listOf(
                        AnalyticsRetrospective(true, 2, 60),
                        AnalyticsRetrospective(true, 4, 180),
                        AnalyticsRetrospective(false, 1, null),
                    ),
                newUserCount = 5,
                returningUserCount = 3,
                retentionEligibleCount = 10,
                retentionReturnedCount = 3,
                retentionObservingCount = 2,
            ),
        )
        val service = AdminAnalyticsService(repository, LocalDateTime.of(2026, 10, 23, 0, 0))

        val result = service.find(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 30))

        assertThat(result.completionRate).isCloseTo(
            200.0 / 3,
            org.assertj.core.data.Offset
                .offset(0.0001),
        )
        assertThat(result.secondCompletionRateWithin14Days).isEqualTo(30.0)
        assertThat(result.averageAnswerCount).isEqualTo(3.0)
        assertThat(result.medianAnswerCount).isEqualTo(3.0)
        assertThat(result.averageElapsedDurationSeconds).isEqualTo(120.0)
        assertThat(result.medianElapsedDurationSeconds).isEqualTo(120.0)
    }
}
