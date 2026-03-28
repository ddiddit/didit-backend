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

@Table(name = "notices")
@Entity
class Notice(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false, length = 255)
    var title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: NoticeStatus = NoticeStatus.DRAFT,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val adminId: UUID,
    @Column(nullable = false)
    var sendPush: Boolean = false,
    @Column
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun isPublished(): Boolean = status == NoticeStatus.PUBLISHED && deletedAt == null

    companion object {
        fun register(
            request: NoticeRegisterRequest,
            adminId: UUID,
        ): Notice {
            require(request.title.isNotBlank()) { "제목은 비어 있을 수 없습니다." }
            require(request.content.isNotBlank()) { "내용은 비어 있을 수 없습니다." }
            return Notice(
                id = UUID.randomUUID(),
                title = request.title,
                content = request.content,
                status = request.status,
                sendPush = request.sendPush,
                adminId = adminId,
                deletedAt = null,
            )
        }
    }

    fun update(request: NoticeRegisterRequest) {
        require(request.title.isNotBlank()) { "제목은 비어 있을 수 없습니다." }
        require(request.content.isNotBlank()) { "내용은 비어 있을 수 없습니다." }

        this.title = request.title
        this.content = request.content
        this.status = request.status
        this.sendPush = request.sendPush
    }

    fun delete() {
        this.deletedAt = LocalDateTime.now()
    }
}
