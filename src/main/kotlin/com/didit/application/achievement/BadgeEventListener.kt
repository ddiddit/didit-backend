package com.didit.application.achievement

import com.didit.application.achievement.provided.BadgeAwarder
import com.didit.domain.retrospect.RetrospectiveCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class BadgeEventListener(
    private val badgeAwarder: BadgeAwarder,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(BadgeEventListener::class.java)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onRetrospectiveCompleted(event: RetrospectiveCompletedEvent) {
        try {
            badgeAwarder.awardBadges(event.userId, event.retroDate)
        } catch (e: Exception) {
            logger.error("배지 부여 실패 - userId: ${event.userId}", e)
        }
    }
}
