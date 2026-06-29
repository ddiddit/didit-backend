package com.didit.application.achievement.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class UserLevelNotFoundException(
    userId: UUID,
) : BusinessException(
        MissionErrorCode.USER_LEVEL_NOT_FOUND,
        "userId: $userId",
    )

class CurrentMissionNotFoundException(
    userId: UUID,
) : BusinessException(
        MissionErrorCode.CURRENT_MISSION_NOT_FOUND,
        "userId: $userId",
    )

class MissionDefinitionNotFoundException(
    level: Int,
) : BusinessException(
        MissionErrorCode.MISSION_DEFINITION_NOT_FOUND,
        "level: $level",
    )
