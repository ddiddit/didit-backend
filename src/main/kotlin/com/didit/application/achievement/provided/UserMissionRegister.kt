package com.didit.application.achievement.provided

import java.util.UUID

interface UserMissionRegister {
    fun ensureInitialized(userId: UUID)

    fun confirmLevelUp(userId: UUID)

    fun retryMission(userId: UUID)
}
