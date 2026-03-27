package com.didit.application.retrospect.required

import com.didit.domain.retrospect.Retrospective
import java.util.UUID

interface RetrospectiveRepository {
    fun save(retrospective: Retrospective): Retrospective

    fun findById(id: UUID): Retrospective?

    fun findByUserId(userId: UUID): Retrospective?

    fun findByUserIdWithChatMessages(userId: UUID): Retrospective?
}
