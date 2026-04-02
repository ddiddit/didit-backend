package com.didit.application.achievement.provided

import com.didit.application.achievement.dto.BadgeResponse
import java.util.UUID

interface BadgeFinder {
    fun findAll(userId: UUID): List<BadgeResponse>

    fun findRecent(userId: UUID): List<BadgeResponse>

    fun findUnnotified(userId: UUID): List<BadgeResponse>
}
