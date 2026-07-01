package com.didit.adapter.integration.scheduler

import com.didit.application.achievement.required.UserMissionRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.achievement.UserMission
import com.didit.domain.shared.ServiceTime
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@Component
class MissionFailureProcessor(
    private val userMissionRepository: UserMissionRepository,
    private val retrospectiveRepository: RetrospectiveRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MissionFailureProcessor::class.java)
    }

    @Transactional
    fun failExpiredLv2(userMission: UserMission) {
        userMission.setFailureWaitingConfirm()
        userMissionRepository.save(userMission)
    }

    @Transactional
    fun failIfNoRetroLastWeek(userMission: UserMission) {
        val lastWeekStart =
            ServiceTime.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)

        val lastWeekRetroCount =
            retrospectiveRepository.countCompletedByUserIdAndPeriod(
                userId = userMission.userId,
                from = ServiceTime.startOfDayUtc(lastWeekStart),
                to = ServiceTime.startOfDayUtc(lastWeekStart.plusWeeks(1)),
            )

        if (lastWeekRetroCount == 0) {
            userMission.setFailureWaitingConfirm()
            userMissionRepository.save(userMission)
            logger.info("연속주차 실패 - userId: ${userMission.userId}, missionId: ${userMission.missionId}")
        }
    }
}
