package com.didit.adapter.integration.scheduler

import com.didit.application.achievement.required.UserMissionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MissionScheduler(
    private val userMissionRepository: UserMissionRepository,
    private val missionFailureProcessor: MissionFailureProcessor,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MissionScheduler::class.java)
    }

    @Scheduled(cron = "0 0 * * * *")
    fun checkExpiredLv2Missions() {
        val expiredMissions = userMissionRepository.findExpiredLv2Missions()

        var count = 0
        expiredMissions.forEach { userMission ->
            runCatching { missionFailureProcessor.failExpiredLv2(userMission) }
                .onSuccess { count++ }
                .onFailure { logger.error("Lv2 만료 미션 처리 실패 - missionId: ${userMission.id}", it) }
        }

        logger.info("Lv2 만료 미션 검사 완료 - count: $count")
    }

    @Scheduled(cron = "0 0 0 ? * MON")
    fun checkConsecutiveWeekFailure() {
        val consecutiveWeekMissions = userMissionRepository.findConsecutiveWeekMissionsInProgress()

        consecutiveWeekMissions.forEach { userMission ->
            runCatching { missionFailureProcessor.failIfNoRetroLastWeek(userMission) }
                .onFailure { logger.error("연속주차 실패 검사 실패 - missionId: ${userMission.id}", it) }
        }

        logger.info("연속주차 실패 검사 완료 - total: ${consecutiveWeekMissions.size}")
    }
}
