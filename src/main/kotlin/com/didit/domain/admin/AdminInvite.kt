package com.didit.domain.admin

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "admin_invites")
@Entity
class AdminInvite(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(columnDefinition = "BINARY(16)", nullable = false, unique = true)
    val token: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val email: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val position: AdminPosition,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val invitedBy: UUID,
    @Column(nullable = false)
    val expiredAt: LocalDateTime,
    @Column
    var usedAt: LocalDateTime? = null,
) : BaseEntity() {
    val isUsed: Boolean get() = usedAt != null

    fun isExpired(now: LocalDateTime = LocalDateTime.now()) = now.isAfter(expiredAt)

    fun use(now: LocalDateTime = LocalDateTime.now()) {
        check(!isUsed) { "이미 사용된 초대입니다." }
        check(!isExpired(now)) { "만료된 초대입니다." }

        usedAt = now
    }

    companion object {
        fun create(request: AdminInviteCreateRequest) =
            AdminInvite(
                email = request.email,
                position = request.position,
                invitedBy = request.invitedBy,
                expiredAt = request.expiredAt,
            )
    }
}
