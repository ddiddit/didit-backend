package com.didit.application.admin.provided

import java.util.UUID

interface AdminBadgeFinder {
    fun findAll(): List<AdminBadgeResult>

    fun findHolders(badgeId: UUID): List<AdminBadgeHolder>
}
