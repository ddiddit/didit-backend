package com.didit.application.achievement.required

import com.didit.domain.achievement.MissionStatus
import com.didit.domain.achievement.MissionType
import com.didit.domain.achievement.UserMission
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
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
        """
        SELECT COUNT(r) FROM Retrospective r
        WHERE r.userId = :userId
        AND r.deletedAt IS NULL
        AND CAST(r.completedAt AS date) BETWEEN :startDate AND :endDate
        """,
    )
    fun countRetrosBetweenDates(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): Int

    @Query(
        """
        SELECT um.* FROM user_mission um
        JOIN mission m ON um.mission_id = m.id
        WHERE um.status = :status
        AND m.level = 2
        AND DATE_ADD(um.started_at, INTERVAL m.duration_days DAY) < CURDATE()
        AND um.progress < m.target_count
        """,
        nativeQuery = true,
    )
    fun findExpiredLv2Missions(
        @Param("status") status: String = "IN_PROGRESS",
    ): List<UserMission>

    @Query(
        """
        SELECT um FROM UserMission um
        WHERE um.status = :status
        AND um.missionId IN (
            SELECT m.id FROM Mission m
            WHERE m.level IN (3, 5, 7, 9)
            AND m.missionType = :missionType
        )
        """,
    )
    fun findConsecutiveWeekMissionsInProgress(
        @Param("status") status: MissionStatus = MissionStatus.IN_PROGRESS,
        @Param("missionType") missionType: MissionType = MissionType.CONSECUTIVE_WEEK,
    ): List<UserMission>
}
