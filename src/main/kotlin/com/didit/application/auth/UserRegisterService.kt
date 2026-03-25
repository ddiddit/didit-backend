package com.didit.application.auth

import com.didit.application.auth.exception.DuplicateNicknameException
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.provided.UserRegister
import com.didit.application.auth.required.UserConsentRepository
import com.didit.application.auth.required.UserRepository
import com.didit.domain.auth.Job
import com.didit.domain.auth.UserConsent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class UserRegisterService(
    private val userFinder: UserFinder,
    private val userRepository: UserRepository,
    private val userConsentRepository: UserConsentRepository,
) : UserRegister {
    @Transactional
    override fun register(
        userId: UUID,
        nickname: String,
        job: Job,
        marketingAgreed: Boolean,
    ) {
        val user = userFinder.findByIdOrThrow(userId)

        user.completeOnboarding(nickname = nickname, job = job)
        userRepository.save(user)

        userConsentRepository.save(
            UserConsent.create(userId = userId, marketingAgreed = marketingAgreed),
        )
    }

    @Transactional
    override fun updateProfile(
        userId: UUID,
        nickname: String,
        job: Job,
    ) {
        if (userRepository.existsByNickname(nickname)) throw DuplicateNicknameException()

        val user = userFinder.findByIdOrThrow(userId)
        user.updateProfile(nickname = nickname, job = job)
        userRepository.save(user)
    }
}
