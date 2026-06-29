package com.didit.application.achievement

import com.didit.application.achievement.required.MissionRepository
import com.didit.application.achievement.required.UserLevelRepository
import com.didit.application.achievement.required.UserMissionRepository
import com.didit.domain.achievement.UserLevel
import com.didit.domain.achievement.UserMission
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class MissionProgressService(
    private val userMissionRepository: UserMissionRepository,
    private val userLevelRepository: UserLevelRepository,
    private val missionRepository: MissionRepository,
    private val missionConditionChecker: MissionConditionChecker,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MissionProgressService::class.java)
    }

    fun updateProgressOnRetroComplete(
        userId: UUID,
        retrospectiveId: UUID,
        retroDate: LocalDate,
    ) {
        val userLevel =
            userLevelRepository.findByUserId(userId) ?: run {
                userLevelRepository.save(UserLevel(userId))
            }

        var currentUserMission = userMissionRepository.findCurrentMissionByUserId(userId)

        if (currentUserMission == null) {
            val lv1Mission =
                missionRepository.findByLevel(1) ?: run {
                    logger.warn("Lv.1 미션 정의가 없습니다")
                    return
                }
            val userMission = UserMission.create(userId, lv1Mission.id)
            userMissionRepository.save(userMission)
            updateProgressOnRetroComplete(userId, retrospectiveId, retroDate)
            return
        }

        val mission =
            missionRepository.findAll().find { it.id == currentUserMission.missionId } ?: run {
                logger.warn("미션 정보를 찾을 수 없습니다 - missionId: ${currentUserMission.missionId}")
                return
            }

        val isCompleted =
            missionConditionChecker.checkAndUpdate(
                userMission = currentUserMission,
                userId = userId,
                retroDate = retroDate,
                mission = mission,
            )

        if (isCompleted) {
            completeMission(currentUserMission, userLevel)
            logger.info(
                "미션 완료 - userId: $userId, level: ${userLevel.currentLevel}, " +
                    "missionType: ${mission.missionType}",
            )
        } else {
            userMissionRepository.save(currentUserMission)
            logger.debug(
                "미션 진행 중 - userId: $userId, level: ${userLevel.currentLevel}, " +
                    "progress: ${currentUserMission.progress}/${mission.targetCount}",
            )
        }
    }

    private fun completeMission(
        userMission: UserMission,
        userLevel: UserLevel,
    ) {
        userMission.complete()
        userMissionRepository.save(userMission)
        awardNextLevel(userLevel, userMission.userId)
    }

    private fun awardNextLevel(
        userLevel: UserLevel,
        userId: UUID,
    ) {
        val currentLevel = userLevel.currentLevel

        if (currentLevel >= 10) {
            logger.info("사용자가 최대 레벨(10)에 도달했습니다 - userId: $userId")
            return
        }

        userLevel.levelUp()
        userLevelRepository.save(userLevel)

        val nextLevel = userLevel.currentLevel
        val nextMission = missionRepository.findByLevel(nextLevel)

        if (nextMission != null) {
            val nextUserMission = UserMission.create(userId, nextMission.id)
            nextUserMission.levelUpPopupShown = false
            userMissionRepository.save(nextUserMission)

            logger.info(
                "레벨업 및 다음 미션 생성 - userId: $userId, " +
                    "currentLevel: $nextLevel, missionType: ${nextMission.missionType}",
            )
        } else {
            logger.warn("Lv.$nextLevel 미션 정의가 없습니다")
        }
    }
}
