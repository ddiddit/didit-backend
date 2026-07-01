package com.didit.application.admin.provided

import java.util.UUID

interface AdminBadgeRegister {
    fun create(command: AdminBadgeCreateCommand): AdminBadgeResult

    fun update(
        badgeId: UUID,
        command: AdminBadgeUpdateCommand,
    ): AdminBadgeResult

    fun changeActive(
        badgeId: UUID,
        active: Boolean,
    ): AdminBadgeResult
}
