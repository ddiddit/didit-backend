package com.didit.domain.auth

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_id"])],
)
@Entity
class User(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 50)
    var nickname: String,
    @Column(length = 100)
    var job: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: Provider,
    @Column(nullable = false)
    val providerId: String,
    var deletedAt: LocalDateTime? = null,
) : BaseEntity() {
    val isDeleted: Boolean get() = deletedAt != null

    fun withdraw(now: LocalDateTime = LocalDateTime.now()) {
        check(!isDeleted) { "이미 탈퇴한 회원입니다." }

        deletedAt = now
    }

    fun updateProfile(
        nickname: String,
        job: String?,
    ) {
        require(nickname.isNotBlank()) { "닉네임은 비어있을 수 없습니다." }

        this.nickname = nickname
        this.job = job
    }

    companion object {
        fun register(
            provider: Provider,
            providerId: String,
            nickname: String,
        ) = User(provider = provider, providerId = providerId, nickname = nickname)
    }
}
