package com.didit.application.auth.required

import com.didit.domain.auth.Provider
import com.didit.domain.auth.User
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
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

    @Query("SELECT u FROM User u WHERE u.deletedAt < :cutoff")
    fun findAllWithdrawnBefore(
        @Param("cutoff") cutoff: LocalDateTime,
    ): List<User>

    fun delete(user: User)
}
