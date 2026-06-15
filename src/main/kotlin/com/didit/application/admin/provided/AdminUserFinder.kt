package com.didit.application.admin.provided

import com.didit.domain.shared.Job
import java.util.UUID

interface AdminUserFinder {
    fun findUsers(
        keyword: String?,
        job: Job?,
        isDeleted: Boolean?,
        page: Int,
    ): UserListResult

    fun findUserDetail(userId: UUID): UserDetailResult
}
