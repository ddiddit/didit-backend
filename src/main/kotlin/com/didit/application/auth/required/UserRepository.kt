package com.didit.application.auth.required

import com.didit.domain.auth.Provider
import com.didit.domain.auth.User
import com.didit.domain.shared.Job
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface UserRepository : Repository<User, UUID> {
    fun save(user: User): User

    fun existsByNicknameAndDeletedAtIsNull(nickname: String): Boolean

    fun existsByNicknameAndIdNotAndDeletedAtIsNull(
        nickname: String,
        userId: UUID,
    ): Boolean

    fun findById(id: UUID): User?

    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId AND u.deletedAt IS NULL")
    fun findByProviderAndProviderId(
        provider: Provider,
        providerId: String,
    ): User?

    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId AND u.deletedAt IS NOT NULL")
    fun findByProviderAndProviderIdAndDeletedAtIsNotNull(
        provider: Provider,
        providerId: String,
    ): User?

    fun findAllByDeletedAtIsNullAndEmailIsNotNull(): List<User>

    fun findAllByIdInAndDeletedAtIsNullAndEmailIsNotNull(ids: List<UUID>): List<User>

    @Query("SELECT u FROM User u WHERE u.deletedAt < :cutoff")
    fun findAllWithdrawnBefore(
        @Param("cutoff") cutoff: LocalDateTime,
    ): List<User>

    @Query("SELECT u FROM User u WHERE u.deletedAt < :cutoff AND u.providerId IS NOT NULL")
    fun findAllWithdrawnAndNotAnonymizedBefore(
        @Param("cutoff") cutoff: LocalDateTime,
    ): List<User>

    @Query("SELECT u FROM User u WHERE u.deletedAt < :cutoff AND u.providerId IS NULL")
    fun findAllWithdrawnAndAnonymizedBefore(
        @Param("cutoff") cutoff: LocalDateTime,
    ): List<User>

    fun delete(user: User)

    fun countByDeletedAtIsNull(): Long

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    fun countByCreatedAtAfterAndDeletedAtIsNull(
        @Param("since") since: LocalDateTime,
    ): Long

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    fun findRecentUsers(pageable: Pageable): List<User>

    @Query(
        """
        SELECT u FROM User u
        WHERE (:keyword IS NULL OR u.email LIKE %:keyword% OR u.nickname LIKE %:keyword%)
        AND (:#{#job} IS NULL OR u.job = :job)
        AND (
            :isDeleted IS NULL
            OR (:isDeleted = true AND u.deletedAt IS NOT NULL)
            OR (:isDeleted = false AND u.deletedAt IS NULL)
        )
        ORDER BY (
            SELECT MAX(a.createdAt) FROM AuditLog a
            WHERE a.actorId = u.id AND a.action = 'USER_LOGGED_IN'
        ) DESC NULLS LAST
        """,
    )
    fun findUsersForAdmin(
        @Param("keyword") keyword: String?,
        @Param("job") job: Job?,
        @Param("isDeleted") isDeleted: Boolean?,
        pageable: Pageable,
    ): Page<User>
}
