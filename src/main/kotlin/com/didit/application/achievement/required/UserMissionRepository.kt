package com.didit.application.achievement.required

import com.didit.domain.achievement.MissionStatus
import com.didit.domain.achievement.MissionType
import com.didit.domain.achievement.UserMission
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime
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
            "WHERE um.userId = :userId AND um.status IN ('IN_PROGRESS', 'WAIT_CONFIRM') " +
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

    // 미션 시작 시각(레벨업 시점) 이후 완료된 회고 수 — 같은 날 레벨업시킨 회고는 제외하기 위해 타임스탬프로 비교
    @Query(
        """
        SELECT COUNT(r) FROM Retrospective r
        WHERE r.userId = :userId
        AND r.deletedAt IS NULL
        AND r.completedAt > :startedAt
        """,
    )
    fun countRetrosAfter(
        @Param("userId") userId: UUID,
        @Param("startedAt") startedAt: LocalDateTime,
    ): Int

    @Query(
        """
        SELECT um.* FROM user_missions um
        JOIN missions m ON um.mission_id = m.id
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

    @Query(
        "SELECT m.level AS level, um.status AS status, COUNT(um) AS count " +
            "FROM UserMission um, Mission m " +
            "WHERE um.missionId = m.id " +
            "GROUP BY m.level, um.status ORDER BY m.level",
    )
    fun countGroupByLevelAndStatus(): List<MissionLevelStatusCount>
}

interface MissionLevelStatusCount {
    fun getLevel(): Int

    fun getStatus(): MissionStatus

    fun getCount(): Long
}
