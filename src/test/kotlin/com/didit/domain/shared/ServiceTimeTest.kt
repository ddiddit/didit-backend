package com.didit.domain.shared

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ServiceTimeTest {
    @Test
    fun `startOfDayUtc - KST 자정을 UTC LocalDateTime으로 변환한다`() {
        val date = LocalDate.of(2026, 6, 23)

        val result = ServiceTime.startOfDayUtc(date)

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 6, 22, 15, 0, 0))
    }

    @Test
    fun `dayRangeUtc - KST 하루를 UTC 반열림 구간으로 변환한다`() {
        val date = LocalDate.of(2026, 6, 23)

        val (from, to) = ServiceTime.dayRangeUtc(date)

        assertThat(from).isEqualTo(LocalDateTime.of(2026, 6, 22, 15, 0, 0))
        assertThat(to).isEqualTo(LocalDateTime.of(2026, 6, 23, 15, 0, 0))
    }

    @Test
    fun `today - JVM 기본 타임존과 무관하게 KST 기준 날짜를 반환한다`() {
        assertThat(ServiceTime.today()).isEqualTo(LocalDate.now(ServiceTime.ZONE))
    }

    @Test
    fun `toServiceDate - UTC 시각을 KST 날짜로 변환한다 (자정 직후 경계)`() {
        val utc = LocalDateTime.of(2026, 2, 28, 15, 30)

        assertThat(ServiceTime.toServiceDate(utc)).isEqualTo(LocalDate.of(2026, 3, 1))
    }
}
