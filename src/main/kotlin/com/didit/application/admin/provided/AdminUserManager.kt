package com.didit.application.admin.provided

import com.didit.domain.auth.UserAccountType
import java.util.UUID

interface AdminUserManager {
    fun changeAccountType(
        adminId: UUID,
        userId: UUID,
        accountType: UserAccountType,
    )

    fun forceWithdraw(
        adminId: UUID,
        userId: UUID,
    )
}
