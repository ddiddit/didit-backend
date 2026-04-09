package com.didit.application.organization.provided

import com.didit.domain.organization.Tag
import java.util.UUID

interface TagFinder {
    fun findAllByUserId(userId: UUID): List<Tag>
}
