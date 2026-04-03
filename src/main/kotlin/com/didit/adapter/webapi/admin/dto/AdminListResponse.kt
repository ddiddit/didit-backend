package com.didit.adapter.webapi.admin.dto

import com.didit.domain.admin.Admin
import com.didit.domain.admin.AdminPosition
import com.didit.domain.admin.AdminRole
import com.didit.domain.admin.AdminStatus
import java.util.UUID

data class AdminListResponse(
    val id: UUID,
    val email: String,
    val role: AdminRole,
    val position: AdminPosition?,
    val status: AdminStatus,
) {
    companion object {
        fun from(admin: Admin) =
            AdminListResponse(
                id = admin.id,
                email = admin.email,
                role = admin.role,
                position = admin.position,
                status = admin.status,
            )
    }
}
