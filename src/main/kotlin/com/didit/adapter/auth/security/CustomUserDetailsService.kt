package com.didit.adapter.auth.security

import com.didit.application.auth.required.UserRepository
import com.didit.application.users.exception.UserNotFoundException
import com.didit.application.users.exception.UserWithdrawException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) {
    fun loadUserById(userId: UUID): CustomUserDetails {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        if (user.deletedAt != null) {
            throw UserWithdrawException()
        }

        return CustomUserDetails(user.id, user.role)
    }
}
