package com.didit.domain.auth.entity

import com.didit.domain.auth.enums.Job
import com.didit.domain.auth.enums.Role
import com.didit.domain.auth.enums.SocialProvider
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
@Table(name = "users")
class User(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    val provider: SocialProvider,
    @Column(nullable = false, unique = true)
    val socialId: String,
    @Column(nullable = true)
    val email: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role = Role.USER,
    @Column(nullable = true)
    val nickname: String? = null,
    @Enumerated(EnumType.STRING)
    var job: Job? = null,
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun withdraw() {
        if (this.deletedAt != null) throw IllegalArgumentException("이미 탈퇴한 사용자입니다.")
        this.deletedAt = LocalDateTime.now()
    }

    fun isDeleted(): Boolean = this.deletedAt != null

    companion object {
        fun create(
            provider: SocialProvider,
            socialId: String,
            email: String?,
        ): User =
            User(
                provider = provider,
                socialId = socialId,
                email = email,
            )
    }
}
