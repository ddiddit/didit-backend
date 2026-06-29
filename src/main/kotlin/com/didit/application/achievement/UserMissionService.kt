package com.didit.application.achievement

import com.didit.application.achievement.exception.CurrentMissionNotFoundException
import com.didit.application.achievement.provided.UserMissionRegister
import com.didit.application.achievement.required.UserMissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class UserMissionService(
    private val userMissionRepository: UserMissionRepository,
) : UserMissionRegister {
    override fun confirmLevelUp(userId: UUID) {
        val userMission =
            userMissionRepository.findCurrentMissionByUserId(userId)
                ?: throw CurrentMissionNotFoundException(userId)

        userMission.setLevelUpPopupShown(true)
        userMissionRepository.save(userMission)
    }
}
