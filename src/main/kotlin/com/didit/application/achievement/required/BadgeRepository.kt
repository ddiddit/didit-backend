package com.didit.application.achievement.required

import com.didit.domain.achievement.Badge
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import java.util.UUID

interface BadgeRepository : Repository<Badge, UUID> {
    @Query("SELECT b FROM Badge b ORDER BY b.createdAt ASC")
    fun findAll(): List<Badge>

    fun save(badge: Badge): Badge
}
