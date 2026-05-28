package com.didit.application.retrospect.provided

import java.util.UUID

interface RetrospectDeletionPort {
    fun deleteByUserId(userId: UUID)
}
