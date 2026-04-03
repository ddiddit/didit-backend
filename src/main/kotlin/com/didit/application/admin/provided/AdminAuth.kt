package com.didit.application.admin.provided

import com.didit.application.admin.dto.AdminRefreshResponse
import com.didit.application.admin.dto.AdminTokenResponse
import java.util.UUID

interface AdminAuth {
    fun login(
        email: String,
        password: String,
    ): AdminTokenResponse

    fun logout(adminId: UUID)

    fun refresh(refreshToken: String): AdminRefreshResponse
}
