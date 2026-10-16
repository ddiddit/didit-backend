package com.didit.application.admin

import com.didit.application.admin.provided.AdminAnalyticsFinder
import com.didit.application.admin.provided.AdminAnalyticsResult
import com.didit.application.admin.required.AdminAnalyticsRepository
import com.didit.application.common.exception.BusinessException
import com.didit.application.common.exception.ErrorCode
import com.didit.domain.shared.ServiceTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
@Transactional(readOnly = true)
class AdminAnalyticsService(
    private val repository: AdminAnalyticsRepository,
    @Value("\${analytics.launch-at-utc}") private val launchAt: LocalDateTime,
) : AdminAnalyticsFinder {
    override fun find(
        from: LocalDate,
        to: LocalDate,
    ): AdminAnalyticsResult {
        if (from.isAfter(to)) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "시작일은 종료일보다 늦을 수 없습니다.")
        }
        if (from.isBefore(to.minusYears(1))) {
            throw BusinessException(ErrorCode.INVALID_REQUEST, "조회 기간은 최대 1년입니다.")
        }

        val snapshot =
            repository.load(
                from = ServiceTime.startOfDayUtc(from),
                to = ServiceTime.startOfDayUtc(to.plusDays(1)),
                launchAt = launchAt,
                now = LocalDateTime.now(ZoneOffset.UTC),
            )
        val completed = snapshot.retrospectives.filter { it.completed }

        return AdminAnalyticsResult(
            from = from,
            to = to,
            retrospectiveCount = snapshot.retrospectives.size.toLong(),
            completedCount = completed.size.toLong(),
            completionRate = percentage(completed.size.toLong(), snapshot.retrospectives.size.toLong()),
            retentionEligibleCount = snapshot.retentionEligibleCount,
            retentionReturnedCount = snapshot.retentionReturnedCount,
            retentionObservingCount = snapshot.retentionObservingCount,
            secondCompletionRateWithin14Days =
                percentage(snapshot.retentionReturnedCount, snapshot.retentionEligibleCount),
            returningUserCount = snapshot.returningUserCount,
            newUserCount = snapshot.newUserCount,
            averageAnswerCount = average(completed.map { it.answerCount.toLong() }),
            medianAnswerCount = median(completed.map { it.answerCount.toLong() }),
            averageElapsedDurationSeconds = average(completed.mapNotNull { it.elapsedDurationSeconds }),
            medianElapsedDurationSeconds = median(completed.mapNotNull { it.elapsedDurationSeconds }),
        )
    }

    private fun percentage(
        numerator: Long,
        denominator: Long,
    ): Double = if (denominator == 0L) 0.0 else numerator.toDouble() / denominator * 100

    private fun average(values: List<Long>): Double? = values.takeIf { it.isNotEmpty() }?.average()

    private fun median(values: List<Long>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle].toDouble() else (sorted[middle - 1] + sorted[middle]) / 2.0
    }
}
