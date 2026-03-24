package com.didit.adapter.auth.persistence

import com.didit.domain.auth.entity.User
import com.didit.domain.auth.enums.SocialProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserJpaRepository : JpaRepository<User, UUID> {
    fun findBySocialIdAndProvider(
        socialId: String,
        provider: SocialProvider,
    ): User?

    fun existsBySocialIdAndProvider(
        socialId: String,
        provider: SocialProvider,
    ): Boolean

    fun existsBySocialIdAndProviderAndDeletedAtIsNull(
        socialId: String,
        provider: SocialProvider,
    ): Boolean

    fun findByIdAndDeletedAtIsNull(id: UUID): User?
}
