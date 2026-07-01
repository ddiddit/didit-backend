package com.didit.adapter.integration.scheduler

import com.didit.application.achievement.provided.AchievementDeletionPort
import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.provided.NotificationDeletionPort
import com.didit.application.organization.provided.OrganizationDeletionPort
import com.didit.application.retrospect.provided.RetrospectDeletionPort
import com.didit.application.retrospect.required.RetrospectiveRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class CleanupExecutor(
    private val retrospectiveRepository: RetrospectiveRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val projectDeletionPort: OrganizationDeletionPort,
    private val notificationDeletionPort: NotificationDeletionPort,
    private val retrospectDeletionPort: RetrospectDeletionPort,
    private val achievementDeletionPort: AchievementDeletionPort,
) {
    @Transactional
    fun cleanPendingRetrospects(): Int {
        val cutoff = LocalDateTime.now().minusDays(1)
        val targets = retrospectiveRepository.findAllPendingBefore(cutoff)
        targets.forEach { retrospectiveRepository.delete(it) }
        return targets.size
    }

    @Transactional
    fun cleanExpiredRefreshTokens(): Int = refreshTokenRepository.deleteAllExpiredBefore(LocalDateTime.now())

    fun findWithdrawnToAnonymize(): List<UUID> =
        userRepository.findAllWithdrawnAndNotAnonymizedBefore(LocalDateTime.now().minusDays(30)).map { it.id }

    fun findWithdrawnToDelete(): List<UUID> =
        userRepository.findAllWithdrawnAndAnonymizedBefore(LocalDateTime.now().minusDays(90)).map { it.id }

    @Transactional
    fun anonymize(userId: UUID) {
        val user = userRepository.findById(userId) ?: return
        user.anonymize()
        userRepository.save(user)
    }

    @Transactional
    fun deleteUser(userId: UUID) {
        val user = userRepository.findById(userId) ?: return
        projectDeletionPort.deleteByUserId(userId)
        notificationDeletionPort.deleteByUserId(userId)
        retrospectDeletionPort.deleteByUserId(userId)
        achievementDeletionPort.deleteByUserId(userId)
        userRepository.delete(user)
    }
}
