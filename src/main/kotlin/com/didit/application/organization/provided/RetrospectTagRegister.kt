package com.didit.application.organization.provided

import java.util.UUID

interface RetrospectTagRegister {
    fun addTag(
        userId: UUID,
        retrospectiveId: UUID,
        tagId: UUID,
    )
}
