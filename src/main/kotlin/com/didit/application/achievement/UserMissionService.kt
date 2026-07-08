package com.didit.application.achievement

import com.didit.application.achievement.exception.CurrentMissionNotFoundException
import com.didit.application.achievement.provided.UserMissionRegister
import com.didit.application.achievement.required.MissionRepository
import com.didit.application.achievement.required.UserLevelRepository
import com.didit.application.achievement.required.UserMissionRepository
import com.didit.domain.achievement.MissionType
import com.didit.domain.achievement.UserLevel
import com.didit.domain.achievement.UserMission
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class UserMissionService(
    private val userMissionRepository: UserMissionRepository,
    private val missionRepository: MissionRepository,
    private val userLevelRepository: UserLevelRepository,
) : UserMissionRegister {
    override fun ensureInitialized(userId: UUID) {
        if (userLevelRepository.existsByUserId(userId)) {
            return
        }
        userLevelRepository.save(UserLevel.create(userId))

        val lv1Mission = missionRepository.findByLevel(1) ?: return
        val userMission = UserMission.create(userId, lv1Mission.id)
        userMission.levelUpPopupShown = true
        userMissionRepository.save(userMission)
    }

    override fun confirmLevelUp(userId: UUID) {
        val userMission =
            userMissionRepository.findCurrentMissionByUserId(userId)
                ?: userMissionRepository.findByUserId(userId).firstOrNull()
                ?: throw CurrentMissionNotFoundException(userId)

        userMission.levelUpPopupShown = true
        userMissionRepository.save(userMission)
    }

    override fun retryMission(userId: UUID) {
        val userMission =
            userMissionRepository.findCurrentMissionByUserId(userId)
                ?: throw CurrentMissionNotFoundException(userId)

        val mission =
            missionRepository.findAll().find { it.id == userMission.missionId }
                ?: throw CurrentMissionNotFoundException(userId)

        userMission.retry()

        if (mission.missionType == MissionType.TIME_LIMITED) {
            userMission.startedAt = LocalDateTime.now()
        }

        userMission.failurePopupShown = true
        userMissionRepository.save(userMission)
    }
}
