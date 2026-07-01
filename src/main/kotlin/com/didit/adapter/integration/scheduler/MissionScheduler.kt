package com.didit.adapter.integration.scheduler

import com.didit.application.achievement.required.MissionRepository
import com.didit.application.achievement.required.UserMissionRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.shared.ServiceTime
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.temporal.TemporalAdjusters

@Component
class MissionScheduler(
    private val userMissionRepository: UserMissionRepository,
    private val missionRepository: MissionRepository,
    private val retrospectiveRepository: RetrospectiveRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MissionScheduler::class.java)
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun checkExpiredLv2Missions() {
        val expiredMissions = userMissionRepository.findExpiredLv2Missions()

        expiredMissions.forEach { userMission ->
            userMission.setFailureWaitingConfirm()
            userMissionRepository.save(userMission)
        }

        logger.info("Lv2 만료 미션 검사 완료 - count: ${expiredMissions.size}")
    }

    @Scheduled(cron = "0 0 0 ? * MON")
    @Transactional
    fun checkConsecutiveWeekFailure() {
        val consecutiveWeekMissions = userMissionRepository.findConsecutiveWeekMissionsInProgress()

        consecutiveWeekMissions.forEach { userMission ->
            val lastWeekStart =
                ServiceTime.today().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).minusWeeks(1)

            val lastWeekRetroCount =
                retrospectiveRepository.countCompletedByUserIdAndPeriod(
                    userId = userMission.userId,
                    from = ServiceTime.startOfDayUtc(lastWeekStart),
                    to = ServiceTime.startOfDayUtc(lastWeekStart.plusWeeks(1)),
                )

            if (lastWeekRetroCount == 0) {
                userMission.setFailureWaitingConfirm()
                userMissionRepository.save(userMission)
                logger.info(
                    "연속주차 실패 - userId: ${userMission.userId}, missionId: ${userMission.missionId}",
                )
            }
        }

        logger.info("연속주차 실패 검사 완료 - total: ${consecutiveWeekMissions.size}")
    }
}
