package com.didit.application.admin.provided

import java.util.UUID

interface AdminBadgeFinder {
    fun findAll(): List<AdminBadgeResult>

    fun findById(badgeId: UUID): AdminBadgeResult

    fun findHolders(badgeId: UUID): List<AdminBadgeHolder>

    fun findMeta(): AdminBadgeMetaResult
}
