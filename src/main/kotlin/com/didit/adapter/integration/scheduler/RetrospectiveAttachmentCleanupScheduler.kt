package com.didit.adapter.integration.scheduler

import com.didit.application.retrospect.RetrospectiveAttachmentCleanup
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RetrospectiveAttachmentCleanupScheduler(
    private val cleanup: RetrospectiveAttachmentCleanup,
) {
    @Scheduled(cron = "0 15 * * * *")
    fun cleanupExpiredAttachments() = cleanup.cleanupExpired()
}
