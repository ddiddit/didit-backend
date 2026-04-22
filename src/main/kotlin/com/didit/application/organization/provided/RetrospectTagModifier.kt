package com.didit.application.organization.provided

import java.util.UUID

interface RetrospectTagModifier {
    fun delete(
        userId: UUID,
        retrospectiveId: UUID,
        tagId: UUID,
    )
}
