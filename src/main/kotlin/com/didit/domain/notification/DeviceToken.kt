package com.didit.domain.notification

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Table(
    name = "device_tokens",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "device_type"])]
)
@Entity
class DeviceToken(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,

    @Column(nullable = false)
    var token: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    val deviceType: DeviceType,
) : BaseEntity()