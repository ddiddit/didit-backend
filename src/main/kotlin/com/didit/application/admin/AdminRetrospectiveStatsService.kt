package com.didit.application.admin

import com.didit.application.admin.provided.AdminRetrospectiveStatsFinder
import com.didit.application.admin.provided.AdminRetrospectiveStatsResult
import com.didit.application.admin.provided.DailyRetroCount
import com.didit.application.auth.required.UserRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.InputType
import com.didit.domain.retrospect.RetroStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class AdminRetrospectiveStatsService(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val userRepository: UserRepository,
) : AdminRetrospectiveStatsFinder {
    override fun getRetrospectiveStats(): AdminRetrospectiveStatsResult {
        val completed = retrospectiveRepository.countByStatusAndDeletedAtIsNull(RetroStatus.COMPLETED)
        val pending = retrospectiveRepository.countByStatusAndDeletedAtIsNull(RetroStatus.PENDING)
        val inProgress = retrospectiveRepository.countByStatusAndDeletedAtIsNull(RetroStatus.IN_PROGRESS)
        val total = completed + pending + inProgress
        val totalUsers = userRepository.countByDeletedAtIsNull()

        // 최근 30일(오늘 포함) 완료 추이
        val thirtyDaysAgo = LocalDate.now().minusDays(29).atStartOfDay()

        return AdminRetrospectiveStatsResult(
            total = total,
            completed = completed,
            inProgress = pending + inProgress,
            completionRate = if (total == 0L) 0.0 else completed.toDouble() / total * 100,
            avgPerUser = if (totalUsers == 0L) 0.0 else completed.toDouble() / totalUsers,
            textAnswerCount = retrospectiveRepository.countUserAnswersByInputType(InputType.TEXT),
            voiceAnswerCount = retrospectiveRepository.countUserAnswersByInputType(InputType.STT),
            dailyTrend =
                retrospectiveRepository
                    .findWeeklyRetroTrend(thirtyDaysAgo)
                    .map { DailyRetroCount(date = it.getDate().toLocalDate(), count = it.getCount()) },
        )
    }
}
