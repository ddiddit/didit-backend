package com.didit.adapter.integration.scheduler

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CleanupScheduler(
    private val cleanupExecutor: CleanupExecutor,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CleanupScheduler::class.java)
    }

    @Scheduled(cron = "0 0 5 * * *")
    fun cleanup() {
        cleanPendingRetrospects()
        cleanExpiredRefreshTokens()
        cleanWithdrawnUsers()
    }

    private fun cleanPendingRetrospects() {
        runCatching { cleanupExecutor.cleanPendingRetrospects() }
            .onSuccess { logger.info("PENDING 회고 삭제 - count: $it") }
            .onFailure { logger.error("PENDING 회고 삭제 실패", it) }
    }

    private fun cleanExpiredRefreshTokens() {
        runCatching { cleanupExecutor.cleanExpiredRefreshTokens() }
            .onSuccess { logger.info("만료 리프레시 토큰 삭제 - count: $it") }
            .onFailure { logger.error("만료 리프레시 토큰 삭제 실패", it) }
    }

    private fun cleanWithdrawnUsers() {
        anonymizeWithdrawnUsers()
        deleteWithdrawnUsers()
    }

    private fun anonymizeWithdrawnUsers() {
        var count = 0
        cleanupExecutor.findWithdrawnToAnonymize().forEach { userId ->
            runCatching { cleanupExecutor.anonymize(userId) }
                .onSuccess { count++ }
                .onFailure { logger.error("탈퇴 유저 익명화 실패 - userId: $userId", it) }
        }
        logger.info("탈퇴 유저 익명화 - count: $count")
    }

    private fun deleteWithdrawnUsers() {
        var count = 0
        cleanupExecutor.findWithdrawnToDelete().forEach { userId ->
            runCatching { cleanupExecutor.deleteUser(userId) }
                .onSuccess { count++ }
                .onFailure { logger.error("탈퇴 유저 삭제 실패 - userId: $userId", it) }
        }
        logger.info("탈퇴 유저 완전 삭제 - count: $count")
    }
}
