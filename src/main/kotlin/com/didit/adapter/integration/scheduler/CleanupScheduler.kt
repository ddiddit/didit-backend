package com.didit.adapter.integration.scheduler

import com.didit.application.auth.required.RefreshTokenRepository
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
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CleanupScheduler::class.java)
    }

    @Scheduled(cron = "0 0 8 * * *")
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
}
