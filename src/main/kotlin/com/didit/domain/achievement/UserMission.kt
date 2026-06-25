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
    @Column(columnDefinition = "JSON")
    var popupStatus: String = """{"levelUpPopupShown":false,"failurePopupShown":false}""",
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

    fun incrementProgress() {
        this.progress += 1
    }

    fun setLevelUpPopupShown(shown: Boolean) {
        val statusMap = parsePopupStatus().toMutableMap()
        statusMap["levelUpPopupShown"] = shown
        popupStatus = statusMap.toJsonString()
    }

    fun setFailurePopupShown(shown: Boolean) {
        val statusMap = parsePopupStatus().toMutableMap()
        statusMap["failurePopupShown"] = shown
        popupStatus = statusMap.toJsonString()
    }

    fun isLevelUpPopupShown(): Boolean = parsePopupStatus()["levelUpPopupShown"] as? Boolean ?: false

    fun isFailurePopupShown(): Boolean = parsePopupStatus()["failurePopupShown"] as? Boolean ?: false

    private fun parsePopupStatus(): Map<String, Any> =
        try {
            val json = popupStatus.removeSurrounding("{", "}")
            json.split(",").associate { pair ->
                val (key, value) = pair.split(":").map { it.trim().trim('"') }
                key to (value.toBoolean() as Any)
            }
        } catch (e: Exception) {
            mapOf("levelUpPopupShown" to false, "failurePopupShown" to false)
        }

    private fun Map<String, Any>.toJsonString(): String = "{" + entries.joinToString(",") { (k, v) -> "\"$k\":$v" } + "}"

    companion object {
        fun create(
            userId: UUID,
            missionId: UUID,
        ): UserMission = UserMission(userId = userId, missionId = missionId)
    }
}
