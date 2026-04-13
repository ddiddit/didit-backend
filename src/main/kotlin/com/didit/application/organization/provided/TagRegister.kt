package com.didit.application.organization.provided

import com.didit.domain.organization.Tag
import java.util.UUID

interface TagRegister {
    fun create(
        userId: UUID,
        name: String,
    ): Tag
}
