package com.didit.application.admin.provided

import com.didit.domain.admin.Admin
import java.util.UUID

interface AdminFinder {
    fun findByIdOrThrow(adminId: UUID): Admin

    fun findAll(): List<Admin>
}
