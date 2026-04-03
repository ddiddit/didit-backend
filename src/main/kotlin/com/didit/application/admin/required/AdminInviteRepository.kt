package com.didit.application.admin.required

import com.didit.domain.admin.AdminInvite
import org.springframework.data.repository.Repository
import java.util.UUID

interface AdminInviteRepository : Repository<AdminInvite, UUID> {
    fun save(invite: AdminInvite): AdminInvite

    fun findByToken(token: UUID): AdminInvite?

    fun existsByEmailAndUsedAtIsNull(email: String): Boolean
}
