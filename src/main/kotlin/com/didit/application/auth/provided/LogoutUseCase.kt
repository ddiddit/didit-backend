package com.didit.application.auth.provided

import com.didit.application.auth.required.RefreshTokenRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LogoutUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    @Transactional
    fun logout(userId: UUID) {
        refreshTokenRepository.deleteByUserId(userId)
    }
}
