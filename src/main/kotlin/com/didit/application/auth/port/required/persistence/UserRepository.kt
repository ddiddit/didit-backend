package com.didit.application.auth.port.required.persistence

import com.didit.domain.auth.entity.User
import com.didit.domain.auth.enums.SocialProvider
import java.util.UUID

interface UserRepository {
    fun findBySocialIdAndProvider(
        socialId: String,
        provider: SocialProvider,
    ): User?

    fun existsBySocialIdAndProvider(
        socialId: String,
        provider: SocialProvider,
    ): Boolean

    fun save(user: User): User

    fun findById(id: UUID): User?
}
