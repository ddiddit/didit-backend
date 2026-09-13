package com.didit.adapter.integration.scheduler

import com.didit.application.retrospect.AttachmentConversationRecovery
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AttachmentConversationRecoveryScheduler(
    private val recovery: AttachmentConversationRecovery,
) {
    @Scheduled(cron = "30 * * * * *")
    fun recoverStaleAttachmentConversations() = recovery.recoverStale()
}
