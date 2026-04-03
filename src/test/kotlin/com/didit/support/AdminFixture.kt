package com.didit.support

import com.didit.domain.admin.Admin
import com.didit.domain.admin.AdminInvite
import com.didit.domain.admin.AdminInviteCreateRequest
import com.didit.domain.admin.AdminPosition
import com.didit.domain.admin.AdminRefreshToken
import com.didit.domain.admin.AdminRegisterRequest
import java.time.LocalDateTime
import java.util.UUID

object AdminFixture {
    fun createSuperAdmin(
        email: String = "super@didit.com",
        encodedPassword: String = "encoded-password",
    ) = Admin.createSuperAdmin(email = email, encodedPassword = encodedPassword)

    fun createAdmin(
        email: String = "admin@didit.com",
        encodedPassword: String = "encoded-password",
        position: AdminPosition = AdminPosition.DEVELOPER,
    ) = Admin.register(
        AdminRegisterRequest(
            email = email,
            encodedPassword = encodedPassword,
            position = position,
        ),
    )
}

object AdminInviteFixture {
    fun create(
        email: String = "invite@didit.com",
        position: AdminPosition = AdminPosition.DEVELOPER,
        invitedBy: UUID = UUID.randomUUID(),
        expiredAt: LocalDateTime = LocalDateTime.now().plusDays(1),
    ) = AdminInvite.create(
        AdminInviteCreateRequest(
            email = email,
            position = position,
            invitedBy = invitedBy,
            expiredAt = expiredAt,
        ),
    )
}

object AdminRefreshTokenFixture {
    fun create(
        adminId: UUID = UUID.randomUUID(),
        token: String = "admin-refresh-token",
        expiresAt: LocalDateTime = LocalDateTime.now().plusDays(14),
    ) = AdminRefreshToken.create(adminId = adminId, token = token, expiresAt = expiresAt)
}
