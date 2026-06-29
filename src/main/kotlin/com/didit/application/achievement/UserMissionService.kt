package com.didit.application.achievement

import com.didit.application.achievement.exception.CurrentMissionNotFoundException
import com.didit.application.achievement.exception.InvalidPopupTypeException
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
    override fun confirmPopup(
        userId: UUID,
        type: String,
    ) {
        val userMission =
            userMissionRepository.findCurrentMissionByUserId(userId)
                ?: throw CurrentMissionNotFoundException(userId)

        when (type) {
            "LEVEL_UP" -> userMission.setLevelUpPopupShown(true)
            "FAILURE" -> userMission.setFailurePopupShown(true)
            else -> throw InvalidPopupTypeException(type)
        }

        userMissionRepository.save(userMission)
    }
}
