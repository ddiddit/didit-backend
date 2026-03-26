package com.didit.application.admin

import com.didit.application.admin.exception.AdminNotFoundException
import com.didit.application.admin.provided.AdminFinder
import com.didit.application.admin.required.AdminRepository
import com.didit.domain.admin.Admin
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class AdminQueryService(
    private val adminRepository: AdminRepository,
) : AdminFinder {
    override fun findByIdOrThrow(adminId: UUID): Admin = adminRepository.findById(adminId) ?: throw AdminNotFoundException(adminId)

    override fun findAll(): List<Admin> = adminRepository.findAll()
}
