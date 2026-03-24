package com.didit.adapter.auth.persistence

import com.didit.domain.auth.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface RefreshTokenJpaRepository : JpaRepository<RefreshToken, UUID> {
    fun findByToken(token: String): RefreshToken?

    fun findByUserId(userId: UUID): RefreshToken?

    @Modifying
    @Query("delete from RefreshToken r where r.userId=:userId")
    fun deleteByUserId(userId: UUID)
}
