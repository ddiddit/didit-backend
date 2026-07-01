package com.didit.application.achievement.required

import com.didit.domain.achievement.Mission
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import java.util.UUID

interface MissionRepository : Repository<Mission, UUID> {
    fun findByLevel(level: Int): Mission?

    @Query("SELECT m FROM Mission m WHERE m.isActive = true ORDER BY m.level ASC")
    fun findAllActive(): List<Mission>

    @Query("SELECT m FROM Mission m ORDER BY m.level ASC")
    fun findAll(): List<Mission>

    fun save(mission: Mission): Mission
}
