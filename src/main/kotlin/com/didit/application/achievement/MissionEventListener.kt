package com.didit.application.achievement

import com.didit.domain.retrospect.RetrospectiveCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MissionEventListener(
    private val missionProgressService: MissionProgressService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MissionEventListener::class.java)
    }

    @EventListener
    @Transactional
    fun onRetrospectiveCompleted(event: RetrospectiveCompletedEvent) {
        try {
            missionProgressService.updateProgressOnRetroComplete(
                userId = event.userId,
                retrospectiveId = event.retrospectiveId,
                retroDate = event.retroDate,
            )
        } catch (e: Exception) {
            logger.error(
                "미션 진행도 업데이트 실패 - userId: ${event.userId}, retrospectiveId: ${event.retrospectiveId}",
                e,
            )
        }
    }
}
