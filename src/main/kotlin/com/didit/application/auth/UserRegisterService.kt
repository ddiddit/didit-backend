package com.didit.application.auth

import com.didit.application.auth.exception.DuplicateNicknameException
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.provided.UserRegister
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.provided.NotificationSettingModifier
import com.didit.domain.shared.Job
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class UserRegisterService(
    private val userFinder: UserFinder,
    private val userRepository: UserRepository,
    private val notificationSettingModifier: NotificationSettingModifier,
) : UserRegister {
    companion object {
        private val logger = LoggerFactory.getLogger(UserRegisterService::class.java)
    }

    @Transactional
    override fun register(
        userId: UUID,
        nickname: String,
        job: Job,
        marketingAgreed: Boolean,
        nightPushAgreed: Boolean,
    ) {
        if (userRepository.existsByNickname(nickname)) throw DuplicateNicknameException()

        val user = userFinder.findByIdOrThrow(userId)

        user.completeOnboarding(nickname = nickname, job = job)

        user.createConsent(marketingAgreed = marketingAgreed)

        userRepository.save(user)

        notificationSettingModifier.updateNightPushConsent(userId, nightPushAgreed)

        logger.info("온보딩 완료 - userId: $userId, nickname: $nickname, job: $job")
    }

    @Transactional
    override fun updateProfile(
        userId: UUID,
        nickname: String,
        job: Job,
    ) {
        if (userRepository.existsByNicknameAndIdNot(nickname, userId)) throw DuplicateNicknameException()

        val user = userFinder.findByIdOrThrow(userId)

        user.updateProfile(nickname = nickname, job = job)

        userRepository.save(user)

        logger.info("프로필 수정 - userId: $userId, nickname: $nickname, job: $job")
    }

    @Transactional
    override fun updateMarketingConsent(
        userId: UUID,
        agreed: Boolean,
    ) {
        val user = userFinder.findByIdOrThrow(userId)

        user.updateMarketingConsent(agreed)

        userRepository.save(user)

        logger.info("마케팅 동의 수정 - userId: $userId, agreed: $agreed")
    }
}
