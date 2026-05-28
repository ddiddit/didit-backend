package com.didit.adapter.integration.scheduler

import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.provided.NotificationDeletionPort
import com.didit.application.organization.provided.OrganizationDeletionPort
import com.didit.application.retrospect.provided.RetrospectDeletionPort
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
    private val projectDeletionPort: OrganizationDeletionPort,
    private val notificationDeletionPort: NotificationDeletionPort,
    private val retrospectDeletionPort: RetrospectDeletionPort,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CleanupScheduler::class.java)
    }

    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    fun cleanup() {
        cleanPendingRetrospects()
        cleanExpiredRefreshTokens()
        cleanWithdrawnUsers()
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
        anonymizeWithdrawnUsers()
        deleteWithdrawnUsers()
    }

    private fun anonymizeWithdrawnUsers() {
        val cutoff = LocalDateTime.now().minusDays(30)
        val targets = userRepository.findAllWithdrawnAndNotAnonymizedBefore(cutoff)

        targets.forEach { user ->
            user.anonymize()
            userRepository.save(user)
        }

        logger.info("탈퇴 유저 익명화 - count: ${targets.size}")
    }

    private fun deleteWithdrawnUsers() {
        val cutoff = LocalDateTime.now().minusDays(90)
        val targets = userRepository.findAllWithdrawnAndAnonymizedBefore(cutoff)

        targets.forEach { user ->
            projectDeletionPort.deleteByUserId(user.id)
            notificationDeletionPort.deleteByUserId(user.id)
            retrospectDeletionPort.deleteByUserId(user.id)
            userRepository.delete(user)
        }
        logger.info("탈퇴 유저 완전 삭제 - count: ${targets.size}")
    }
}
