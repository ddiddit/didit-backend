package com.didit.application.achievement

import com.didit.application.achievement.required.DailyAccessStreakRepository
import com.didit.domain.achievement.DailyAccessStreak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class DailyAccessTrackingServiceTest {
    @Mock lateinit var dailyAccessStreakRepository: DailyAccessStreakRepository

    private lateinit var service: DailyAccessTrackingService

    private val userId = UUID.randomUUID()
    private val today = LocalDate.now()

    @BeforeEach
    fun setUp() {
        service = DailyAccessTrackingService(dailyAccessStreakRepository)
    }

    @Test
    fun `recordAccess - 스트릭이 없으면 새로 생성한다`() {
        whenever(dailyAccessStreakRepository.findByUserId(userId)).thenReturn(null)
        whenever(dailyAccessStreakRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = service.recordAccess(userId, today)

        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.currentStreak).isEqualTo(1)
    }

    @Test
    fun `recordAccess - 기존 스트릭의 연속일을 증가시킨다`() {
        val existing =
            DailyAccessStreak.create(userId).apply {
                recordAccess(today.minusDays(1))
            }
        whenever(dailyAccessStreakRepository.findByUserId(userId)).thenReturn(existing)
        whenever(dailyAccessStreakRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = service.recordAccess(userId, today)

        assertThat(result.currentStreak).isEqualTo(2)
    }

    @Test
    fun `recordAccess - 같은 날 중복 호출 시 currentStreak이 유지된다`() {
        val existing =
            DailyAccessStreak.create(userId).apply {
                recordAccess(today)
            }
        whenever(dailyAccessStreakRepository.findByUserId(userId)).thenReturn(existing)
        whenever(dailyAccessStreakRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = service.recordAccess(userId, today)

        assertThat(result.currentStreak).isEqualTo(1)
    }
}
