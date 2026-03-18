package com.didit.domain.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime
import java.util.UUID

@Entity
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: UUID,
    @Column(name = "token", nullable = false, length = 500)
    var token: String,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,
)
