package com.didit.application.admin

import com.didit.application.admin.provided.AdminStatsFinder
import com.didit.application.admin.provided.AdminStatsResult
import com.didit.application.admin.provided.RecentInquirySummary
import com.didit.application.admin.provided.RecentUserSummary
import com.didit.application.inquiry.required.InquiryRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.inquiry.InquiryStatus
import com.didit.domain.retrospect.RetroStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@Transactional(readOnly = true)
@Service
class AdminStatsService(
    private val userRepository: UserRepository,
    private val inquiryRepository: InquiryRepository,
    private val retrospectiveRepository: RetrospectiveRepository,
) : AdminStatsFinder {
    override fun getStats(): AdminStatsResult {
        val todayStart = LocalDate.now().atStartOfDay()

        val totalUsers = userRepository.countByDeletedAtIsNull()
        val newUsersToday = userRepository.countByCreatedAtAfter(todayStart)
        val totalRetrospects = retrospectiveRepository.countByStatusAndDeletedAtIsNull(RetroStatus.COMPLETED)
        val unansweredInquiries = inquiryRepository.countByStatusAndDeletedAtIsNull(InquiryStatus.PENDING)

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
            recentUsers = recentUsers,
            recentInquiries = recentInquiries,
        )
    }
}
