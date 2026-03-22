package com.didit.domain.notification

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Table(name = "notification_histories")
@Entity
class NotificationHistory(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationType,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false)
    val body: String,
    @Column(nullable = false)
    var isRead: Boolean = false,
) : BaseEntity() {
    fun read() {
        this.isRead = true
    }

    companion object {
        fun create(request: NotificationHistoryCreateRequest): NotificationHistory =
            NotificationHistory(
                userId = request.userId,
                type = request.type,
                title = request.title,
                body = request.body,
            )
    }
}
