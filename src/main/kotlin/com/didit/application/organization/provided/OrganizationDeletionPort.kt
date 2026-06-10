package com.didit.application.organization.provided

import java.util.UUID

interface OrganizationDeletionPort {
    fun deleteByUserId(userId: UUID)
}
