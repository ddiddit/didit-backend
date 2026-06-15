package com.didit.application.admin

import com.didit.application.admin.provided.AdminUserFinder
import com.didit.application.admin.provided.UserDetailResult
import com.didit.application.admin.provided.UserListResult
import com.didit.application.admin.provided.UserSummary
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditReader
import com.didit.application.auth.exception.UserNotFoundException
import com.didit.application.auth.required.UserRepository
import com.didit.domain.shared.Job
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class AdminUserQueryService(
    private val userRepository: UserRepository,
    private val auditReader: AuditReader,
) : AdminUserFinder {
    companion object {
        private const val PAGE_SIZE = 20

        private val TIMELINE_ACTIONS =
            listOf(
                AuditAction.USER_LOGGED_IN,
                AuditAction.USER_SIGNED_UP,
                AuditAction.USER_PROFILE_UPDATED,
                AuditAction.USER_WITHDREW,
                AuditAction.RETROSPECTIVE_STARTED,
                AuditAction.RETROSPECTIVE_SAVED,
                AuditAction.RETROSPECTIVE_DELETED,
                AuditAction.BADGE_ACQUIRED,
                AuditAction.USER_MARKETING_CONSENT_UPDATED,
            )
    }

    override fun findUsers(
        keyword: String?,
        job: Job?,
        isDeleted: Boolean?,
        page: Int,
    ): UserListResult {
        val pageable = PageRequest.of(page, PAGE_SIZE)
        val userPage = userRepository.findUsersForAdmin(keyword, job, isDeleted, pageable)

        val userIds = userPage.content.map { it.id }
        val lastLoginMap = auditReader.findLastLoginsByUserIds(userIds)

        return UserListResult(
            content = userPage.content.map { UserSummary.from(it, lastLoginMap[it.id]) },
            page = userPage.number,
            size = userPage.size,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages,
        )
    }

    override fun findUserDetail(userId: UUID): UserDetailResult {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)

        val lastLoginAt = auditReader.findLastLogin(userId)

        val timeline = auditReader.findTimeline(userId, TIMELINE_ACTIONS, 20)

        return UserDetailResult(
            profile = UserSummary.from(user, lastLoginAt),
            timeline = timeline,
        )
    }
}
