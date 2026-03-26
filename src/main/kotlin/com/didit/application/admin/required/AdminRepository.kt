package com.didit.application.admin.required

import com.didit.domain.admin.Admin
import org.springframework.data.repository.Repository
import java.util.UUID

interface AdminRepository : Repository<Admin, UUID> {
    fun save(admin: Admin): Admin

    fun findById(id: UUID): Admin?

    fun findByEmail(email: String): Admin?

    fun findAll(): List<Admin>

    fun delete(admin: Admin)
}
