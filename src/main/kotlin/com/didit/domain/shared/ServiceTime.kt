package com.didit.domain.shared

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

object ServiceTime {
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")

    fun today(): LocalDate = LocalDate.now(ZONE)

    fun startOfDayUtc(date: LocalDate): LocalDateTime = date.atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()

    fun dayRangeUtc(date: LocalDate): Pair<LocalDateTime, LocalDateTime> = startOfDayUtc(date) to startOfDayUtc(date.plusDays(1))
}
