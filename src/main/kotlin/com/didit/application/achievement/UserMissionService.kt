package com.didit.application.achievement

import com.didit.application.achievement.exception.CurrentMissionNotFoundException
import com.didit.application.achievement.provided.UserMissionRegister
import com.didit.application.achievement.required.MissionRepository
import com.didit.application.achievement.required.UserMissionRepository
import com.didit.domain.achievement.MissionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class UserMissionService(
    private val userMissionRepository: UserMissionRepository,
    private val missionRepository: MissionRepository,
) : UserMissionRegister {
    override fun confirmLevelUp(userId: UUID) {
        val userMission =
            userMissionRepository.findCurrentMissionByUserId(userId)
                ?: throw CurrentMissionNotFoundException(userId)

        userMission.setLevelUpPopupShown(true)
        userMissionRepository.save(userMission)
    }

    override fun retryMission(userId: UUID) {
        val userMission =
            userMissionRepository.findCurrentMissionByUserId(userId)
                ?: throw CurrentMissionNotFoundException(userId)

        val mission =
            missionRepository.findAll().find { it.id == userMission.missionId }
                ?: throw CurrentMissionNotFoundException(userId)

        when (mission.missionType) {
            MissionType.TIME_LIMITED -> {
                userMission.resetProgress()
                userMission.startedAt = LocalDateTime.now()
            }

            MissionType.CONSECUTIVE_WEEK -> {
                userMission.resetProgress()
            }

            else -> {
                userMission.resetProgress()
            }
        }

        userMission.setFailurePopupShown(true)
        userMissionRepository.save(userMission)
    }
}
