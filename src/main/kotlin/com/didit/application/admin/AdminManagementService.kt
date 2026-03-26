package com.didit.application.admin

import com.didit.application.admin.provided.AdminFinder
import com.didit.application.admin.provided.AdminManager
import com.didit.application.admin.required.AdminRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class AdminManagementService(
    private val adminRepository: AdminRepository,
    private val adminFinder: AdminFinder,
) : AdminManager {
    @Transactional
    override fun approve(adminId: UUID) {
        val admin = adminFinder.findByIdOrThrow(adminId)
        admin.approve()
        adminRepository.save(admin)
    }

    @Transactional
    override fun reject(adminId: UUID) {
        val admin = adminFinder.findByIdOrThrow(adminId)
        admin.reject()
        adminRepository.save(admin)
    }

    @Transactional
    override fun delete(adminId: UUID) {
        val admin = adminFinder.findByIdOrThrow(adminId)
        adminRepository.delete(admin)
    }
}
