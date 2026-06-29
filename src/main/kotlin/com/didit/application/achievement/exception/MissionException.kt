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

class UserMissionNotFoundException(
    missionId: UUID,
) : BusinessException(
        MissionErrorCode.USER_MISSION_NOT_FOUND,
        "missionId: $missionId",
    )

class InvalidPopupTypeException(
    type: String,
) : BusinessException(
        MissionErrorCode.INVALID_POPUP_TYPE,
        "type: $type",
    )
