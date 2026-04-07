package com.didit.application.auth.required

import com.didit.domain.auth.Provider
import com.didit.domain.auth.User
import org.springframework.data.repository.Repository
import java.util.UUID

interface UserRepository : Repository<User, UUID> {
    fun save(user: User): User

    fun existsByNicknameAndDeletedAtIsNull(nickname: String): Boolean

    fun existsByNicknameAndIdNotAndDeletedAtIsNull(
        nickname: String,
        userId: UUID,
    ): Boolean

    fun findById(id: UUID): User?

    fun findByProviderAndProviderId(
        provider: Provider,
        providerId: String,
    ): User?
}
