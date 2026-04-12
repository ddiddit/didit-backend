package com.didit.adapter.integration.scheduler

import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.required.NotificationHistoryRepository
import com.didit.application.notification.required.NotificationSettingRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CleanupScheduler(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val notificationHistoryRepository: NotificationHistoryRepository,
    private val notificationSettingRepository: NotificationSettingRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CleanupScheduler::class.java)
    }

    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    fun cleanup() {
        cleanPendingRetrospects()
        cleanExpiredRefreshTokens()
    }

    private fun cleanPendingRetrospects() {
        val cutoff = LocalDateTime.now().minusDays(1)

        val targets = retrospectiveRepository.findAllPendingBefore(cutoff)

        targets.forEach { retrospectiveRepository.delete(it) }

        logger.info("PENDING 회고 삭제 - count: ${targets.size}")
    }

    private fun cleanExpiredRefreshTokens() {
        val count = refreshTokenRepository.deleteAllExpiredBefore(LocalDateTime.now())

        logger.info("만료 리프레시 토큰 삭제 - count: $count")
    }

    private fun cleanWithdrawnUsers() {
        val cutoff = LocalDateTime.now().minusDays(30)
        val targets = userRepository.findAllWithdrawnBefore(cutoff)

        targets.forEach { user ->
            notificationHistoryRepository.deleteAllByUserId(user.id)
            notificationSettingRepository.deleteByUserId(user.id)
            retrospectiveRepository
                .findAllByUserId(user.id)
                .forEach { retrospectiveRepository.delete(it) }
            userRepository.delete(user)
        }

        logger.info("탈퇴 유저 삭제 - count: ${targets.size}")
    }
}
