package com.didit.domain.notification

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalTime
import java.util.UUID

@Table(name = "notification_settings")
@Entity
class NotificationSetting(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Column(nullable = false)
    var nightPushConsent: Boolean = false,
    @Column(nullable = false)
    var enabled: Boolean = false,
    @Column(nullable = false)
    var reminderTime: LocalTime = LocalTime.of(20, 0),
) : BaseEntity() {
    fun updateSetting(
        enabled: Boolean,
        reminderTime: LocalTime,
        nightPushConsent: Boolean,
    ) {
        val requiresNightConsent = isNightTime(reminderTime) && nightPushConsent.not()
        require(!requiresNightConsent) { "야간 시간대 알림은 야간 푸시 동의가 필요합니다." }
        this.enabled = enabled
        this.reminderTime = reminderTime
    }

    fun updateNightPushConsent(consent: Boolean) {
        this.nightPushConsent = consent
    }

    companion object {
        private val NIGHT_START = LocalTime.of(21, 0)
        private val NIGHT_END = LocalTime.of(8, 0)

        fun create(userId: UUID): NotificationSetting = NotificationSetting(userId = userId)

        fun isNightTime(time: LocalTime): Boolean = time >= NIGHT_START || time < NIGHT_END
    }
}
