package com.didit.domain.admin

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Table(name = "admins")
@Entity
class Admin(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true)
    val email: String,
    @Column(nullable = false)
    var password: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: AdminRole,
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val position: AdminPosition? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AdminStatus = AdminStatus.PENDING,
) : BaseEntity() {
    val isActive: Boolean get() = status == AdminStatus.ACTIVE
    val isPending: Boolean get() = status == AdminStatus.PENDING

    fun approve() {
        check(isPending) { "대기 중인 어드민이 아닙니다." }

        status = AdminStatus.ACTIVE
    }

    fun reject() {
        check(isPending) { "대기 중인 어드민이 아닙니다." }

        status = AdminStatus.REJECTED
    }

    companion object {
        fun createSuperAdmin(
            email: String,
            encodedPassword: String,
        ) = Admin(
            email = email,
            password = encodedPassword,
            role = AdminRole.SUPER_ADMIN,
            status = AdminStatus.ACTIVE,
        )

        fun register(request: AdminRegisterRequest) =
            Admin(
                email = request.email,
                password = request.encodedPassword,
                role = AdminRole.ADMIN,
                position = request.position,
                status = AdminStatus.PENDING,
            )
    }
}
