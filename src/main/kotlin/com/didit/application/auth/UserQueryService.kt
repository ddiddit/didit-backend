package com.didit.application.auth

import com.didit.application.auth.exception.UserNotFoundException
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.required.UserRepository
import com.didit.domain.auth.User
import com.didit.domain.shared.Job
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class UserQueryService(
    private val userRepository: UserRepository,
) : UserFinder {
    override fun findByIdOrThrow(userId: UUID): User =
        userRepository.findById(userId) ?: throw UserNotFoundException(
            userId,
        )

    override fun existsByNickname(nickname: String): Boolean = userRepository.existsByNickname(nickname)

    override fun getJobByUserId(userId: UUID): Job? = (userRepository.findById(userId) ?: throw UserNotFoundException(userId)).job
}
