package com.didit.application.achievement.provided

import java.util.UUID

interface UserMissionRegister {
    fun confirmPopup(
        userId: UUID,
        type: String,
    )
}
