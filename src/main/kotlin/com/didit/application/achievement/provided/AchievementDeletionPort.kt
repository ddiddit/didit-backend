package com.didit.application.achievement.provided

import java.util.UUID

interface AchievementDeletionPort {
    fun deleteByUserId(userId: UUID)
}
