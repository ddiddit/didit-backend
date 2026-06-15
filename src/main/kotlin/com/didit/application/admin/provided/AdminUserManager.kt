package com.didit.application.admin.provided

import java.util.UUID

interface AdminUserManager {
    fun forceWithdraw(
        adminId: UUID,
        userId: UUID,
    )
}
