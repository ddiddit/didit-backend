package com.didit.application.users.provided

import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.users.exception.UserNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class WithdrawUseCase(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    @Transactional
    fun execute(userId: UUID) {
        val user =
            userRepository.findByIdAndDeletedAtIsNull(userId)
                ?: throw UserNotFoundException(userId)

        refreshTokenRepository.deleteByUserId(userId)

        user.withdraw()

        userRepository.save(user)
    }
}
