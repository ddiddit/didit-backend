package com.didit.application.achievement.required

import com.didit.domain.achievement.Badge
import org.springframework.data.repository.Repository
import java.util.UUID

interface BadgeRepository : Repository<Badge, UUID> {
    fun findAll(): List<Badge>

    fun saveAll(badges: List<Badge>): List<Badge>

    fun count(): Long
}
