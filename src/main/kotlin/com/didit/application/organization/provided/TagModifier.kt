package com.didit.application.organization.provided

import java.util.UUID

interface TagModifier {
    fun delete(
        userId: UUID,
        tagId: UUID,
    )
}
