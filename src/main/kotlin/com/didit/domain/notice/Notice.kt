package com.didit.domain.notice

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "notices")
class Notice(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(name = "title", nullable = false, length = 255)
    var title: String,
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: NoticeStatus = NoticeStatus.DRAFT,
    @Column(name = "send_push", nullable = false)
    var sendPush: Boolean = false,
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun isPublished(): Boolean = status == NoticeStatus.PUBLISHED && deletedAt == null
}
