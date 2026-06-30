package com.didit.domain.achievement

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "user_missions")
@Entity
class UserMission(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val missionId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MissionStatus = MissionStatus.IN_PROGRESS,
    @Column(nullable = false)
    var progress: Int = 0,
    @Column
    var lastRetroDate: LocalDate? = null,
    @Column(nullable = false)
    var failureCount: Int = 0,
    @Column(nullable = false)
    var levelUpPopupShown: Boolean = false,
    @Column(nullable = false)
    var failurePopupShown: Boolean = false,
    @Column(name = "started_at", nullable = false)
    var startedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun complete() {
        this.status = MissionStatus.COMPLETED
        this.completedAt = LocalDateTime.now()
    }

    fun fail() {
        this.status = MissionStatus.FAILED
        this.failureCount += 1
    }

    fun resetProgress() {
        this.progress = 0
        this.failureCount += 1
    }

    fun retry() {
        this.status = MissionStatus.IN_PROGRESS
        this.progress = 0
        this.failureCount += 1
        this.completedAt = null
        this.lastRetroDate = null
    }

    fun incrementProgress() {
        this.progress += 1
    }

    fun setFailureWaitingConfirm() {
        this.status = MissionStatus.WAIT_CONFIRM
        this.failurePopupShown = false
    }

    companion object {
        fun create(
            userId: UUID,
            missionId: UUID,
        ): UserMission = UserMission(userId = userId, missionId = missionId)
    }
}
