package com.didit.application.achievement

import com.didit.application.achievement.exception.InvalidPopupTypeException
import com.didit.application.achievement.exception.UserMissionNotFoundException
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
        missionId: UUID,
        type: String,
    ) {
        val userMission =
            userMissionRepository.findByIdAndUserId(missionId, userId)
                ?: throw UserMissionNotFoundException(missionId)

        when (type) {
            "LEVEL_UP" -> userMission.setLevelUpPopupShown(true)
            "FAILURE" -> userMission.setFailurePopupShown(true)
            else -> throw InvalidPopupTypeException(type)
        }

        userMissionRepository.save(userMission)
    }
}
