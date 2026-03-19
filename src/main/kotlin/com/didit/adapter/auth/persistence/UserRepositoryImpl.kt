package com.didit.adapter.auth.persistence

import com.didit.application.auth.required.UserRepository
import com.didit.domain.auth.entity.User
import com.didit.domain.auth.enums.SocialProvider
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun findBySocialIdAndProvider(
        socialId: String,
        provider: SocialProvider,
    ): User? = userJpaRepository.findBySocialIdAndProvider(socialId, provider)

    override fun existsBySocialIdAndProvider(
        socialId: String,
        provider: SocialProvider,
    ): Boolean = userJpaRepository.existsBySocialIdAndProvider(socialId, provider)

    override fun save(user: User): User = userJpaRepository.save(user)

    override fun findById(id: UUID): User? = userJpaRepository.findById(id).orElse(null)
}
