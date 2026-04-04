package com.didit.application.organization.required

import com.didit.domain.organization.Project
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import java.util.UUID

interface ProjectRepository : Repository<Project, UUID> {
    fun save(project: Project): Project

    fun existsByUserIdAndNameAndDeletedAtIsNull(
        userId: UUID,
        name: String,
    ): Boolean

    fun findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId: UUID): List<Project>

    fun findAllByUserIdAndDeletedAtIsNull(userId: UUID): List<Project>

    fun findByIdAndUserIdAndDeletedAtIsNull(
        id: UUID,
        userId: UUID,
    ): Project?

    @Query(
        """
        SELECT p
        FROM Project p
        WHERE p.userId = :userId
          AND p.deletedAt IS NULL
        ORDER BY
          CASE WHEN p.displayOrder IS NULL THEN 1 ELSE 0 END,
          p.displayOrder ASC,
          p.createdAt DESC
    """,
    )
    fun findAllForUser(userId: UUID): List<Project>

    @Query(
        """
        SELECT MAX(p.displayOrder)
        FROM Project p
        WHERE p.userId = :userId AND p.deletedAt IS NULL
    """,
    )
    fun findMaxDisplayOrder(userId: UUID): Int?
}
