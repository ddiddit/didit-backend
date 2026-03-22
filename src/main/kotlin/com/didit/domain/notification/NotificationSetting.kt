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
    var marketingConsent: Boolean = false,

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
    ) {
        this.enabled = enabled
        this.reminderTime = reminderTime
    }

    fun updateMarketingConsent(consent: Boolean) {
        this.marketingConsent = consent
    }

    fun updateNightPushConsent(consent: Boolean) {
        this.nightPushConsent = consent
    }

    companion object {
        fun create(userId: UUID): NotificationSetting =
            NotificationSetting(userId = userId)
    }
}