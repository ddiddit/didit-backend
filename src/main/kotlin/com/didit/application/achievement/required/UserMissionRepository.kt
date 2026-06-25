package com.didit.application.achievement.required

import com.didit.domain.achievement.MissionStatus
import com.didit.domain.achievement.UserMission
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import java.time.LocalDate
import java.util.UUID

interface UserMissionRepository : Repository<UserMission, UUID> {
    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): UserMission?

    @Query(
        "SELECT um FROM UserMission um " +
            "WHERE um.userId = :userId AND um.status = :status " +
            "ORDER BY um.createdAt DESC LIMIT 1",
    )
    fun findByUserIdAndStatus(
        userId: UUID,
        status: MissionStatus,
    ): UserMission?

    @Query(
        "SELECT um FROM UserMission um " +
            "WHERE um.userId = :userId AND um.status = 'IN_PROGRESS' " +
            "ORDER BY um.createdAt DESC LIMIT 1",
    )
    fun findCurrentMissionByUserId(userId: UUID): UserMission?

    fun save(userMission: UserMission): UserMission

    @Query("SELECT um FROM UserMission um WHERE um.userId = :userId ORDER BY um.createdAt DESC")
    fun findByUserId(userId: UUID): List<UserMission>

    @Query(
        "SELECT COUNT(r) FROM Retrospective r " +
            "WHERE r.userId = :userId " +
            "AND r.deletedAt IS NULL " +
            "AND DATE(r.completedAt) BETWEEN :startDate AND :endDate",
    )
    fun countRetrosBetweenDates(
        userId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Int
}
