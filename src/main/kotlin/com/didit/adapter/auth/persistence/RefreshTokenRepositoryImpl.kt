package com.didit.adapter.auth.persistence

import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.domain.auth.entity.RefreshToken
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class RefreshTokenRepositoryImpl(
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository,
) : RefreshTokenRepository {
    override fun save(refreshToken: RefreshToken): RefreshToken = refreshTokenJpaRepository.save(refreshToken)

    override fun findByToken(token: String): RefreshToken? = refreshTokenJpaRepository.findByToken(token)

    override fun findByUserId(userId: UUID): RefreshToken? = refreshTokenJpaRepository.findByUserId(userId)

    override fun deleteByUserId(userId: UUID) {
        refreshTokenJpaRepository.deleteByUserId(userId)
    }
}
