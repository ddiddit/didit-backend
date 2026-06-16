package com.didit.application.admin

import com.didit.application.admin.provided.AdminStatsFinder
import com.didit.application.admin.provided.AdminStatsResult
import com.didit.application.admin.provided.DailyRetroCount
import com.didit.application.admin.provided.RecentInquirySummary
import com.didit.application.admin.provided.RecentUserSummary
import com.didit.application.audit.AuditReader
import com.didit.application.inquiry.required.InquiryRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.inquiry.InquiryStatus
import com.didit.domain.retrospect.RetroStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Transactional(readOnly = true)
@Service
class AdminStatsService(
    private val userRepository: UserRepository,
    private val inquiryRepository: InquiryRepository,
    private val retrospectiveRepository: RetrospectiveRepository,
    private val auditReader: AuditReader,
) : AdminStatsFinder {
    override fun getStats(): AdminStatsResult {
        val now = LocalDateTime.now()
        val todayStart = LocalDate.now().atStartOfDay()
        val todayEnd = LocalDate.now().atTime(LocalTime.MAX)
        val sevenDaysAgo = now.minusDays(7)

        val totalUsers = userRepository.countByDeletedAtIsNull()
        val newUsersToday = userRepository.countByCreatedAtAfter(todayStart)
        val totalRetrospects = retrospectiveRepository.countByStatusAndDeletedAtIsNull(RetroStatus.COMPLETED)
        val unansweredInquiries = inquiryRepository.countByStatusAndDeletedAtIsNull(InquiryStatus.PENDING)
        val dau = auditReader.countDau(todayStart)
        val todayRetrospects = retrospectiveRepository.countByCompletedAtBetweenAndDeletedAtIsNull(todayStart, todayEnd)

        val weeklyRetroTrend =
            retrospectiveRepository
                .findWeeklyRetroTrend(sevenDaysAgo)
                .map { DailyRetroCount(date = it.getDate().toLocalDate(), count = it.getCount()) }

        val recentUsers =
            userRepository
                .findRecentUsers(PageRequest.of(0, 5))
                .map {
                    RecentUserSummary(
                        id = it.id,
                        email = it.email,
                        nickname = it.nickname,
                        job = it.job?.name,
                        createdAt = it.createdAt,
                    )
                }

        val recentInquiries =
            inquiryRepository
                .findAllByDeletedAtIsNullOrderByCreatedAtDesc()
                .take(5)
                .map {
                    RecentInquirySummary(
                        id = it.id,
                        type = it.type.name,
                        content = it.content,
                        status = it.status.name,
                        createdAt = it.createdAt,
                    )
                }

        return AdminStatsResult(
            totalUsers = totalUsers,
            newUsersToday = newUsersToday,
            totalRetrospects = totalRetrospects,
            unansweredInquiries = unansweredInquiries,
            dau = dau,
            todayRetrospects = todayRetrospects,
            weeklyRetroTrend = weeklyRetroTrend,
            recentUsers = recentUsers,
            recentInquiries = recentInquiries,
        )
    }
}
