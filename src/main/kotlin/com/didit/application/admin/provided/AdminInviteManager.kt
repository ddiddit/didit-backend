package com.didit.application.admin.provided

import com.didit.domain.admin.AdminPosition
import java.util.UUID

interface AdminInviteManager {
    fun invite(
        invitedBy: UUID,
        email: String,
        position: AdminPosition,
    )

    fun register(
        token: UUID,
        email: String,
        password: String,
    )
}
