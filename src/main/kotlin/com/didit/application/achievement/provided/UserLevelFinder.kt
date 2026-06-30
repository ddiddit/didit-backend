package com.didit.application.achievement.provided

import java.util.UUID

interface UserLevelFinder {
    fun getCurrentLevel(userId: UUID): Int
}
