package com.didit.application.admin.required

import com.didit.domain.admin.AdminRefreshToken
import org.springframework.data.repository.Repository
import java.util.UUID

interface AdminRefreshTokenRepository : Repository<AdminRefreshToken, UUID> {
    fun save(token: AdminRefreshToken): AdminRefreshToken

    fun findByToken(token: String): AdminRefreshToken?

    fun deleteByAdminId(adminId: UUID)
}
