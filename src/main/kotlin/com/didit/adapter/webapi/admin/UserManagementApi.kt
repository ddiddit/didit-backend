package com.didit.adapter.webapi.admin

import com.didit.adapter.webapi.admin.annotation.CurrentAdminId
import com.didit.adapter.webapi.admin.annotation.RequireAdmin
import com.didit.adapter.webapi.admin.annotation.RequireSuperAdmin
import com.didit.adapter.webapi.admin.dto.UserDetailResponse
import com.didit.adapter.webapi.admin.dto.UserPageResponse
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.application.admin.provided.AdminUserFinder
import com.didit.application.admin.provided.AdminUserManager
import com.didit.domain.shared.Job
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/admin/users")
@RestController
class UserManagementApi(
    private val adminUserFinder: AdminUserFinder,
    private val adminUserManager: AdminUserManager,
) {
    @RequireAdmin
    @GetMapping
    fun findUsers(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) job: Job?,
        @RequestParam(required = false) isDeleted: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
    ): SuccessResponse<UserPageResponse> {
        val result = adminUserFinder.findUsers(keyword, job, isDeleted, page)
        return SuccessResponse.of(UserPageResponse.from(result))
    }

    @RequireAdmin
    @GetMapping("/{userId}")
    fun findUser(
        @PathVariable userId: UUID,
    ): SuccessResponse<UserDetailResponse> {
        val result = adminUserFinder.findUserDetail(userId)
        return SuccessResponse.of(UserDetailResponse.from(result))
    }

    @RequireSuperAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{userId}/force-withdraw")
    fun forceWithdraw(
        @CurrentAdminId adminId: UUID,
        @PathVariable userId: UUID,
    ) {
        adminUserManager.forceWithdraw(adminId = adminId, userId = userId)
    }
}
