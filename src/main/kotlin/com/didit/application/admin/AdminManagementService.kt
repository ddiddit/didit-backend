package com.didit.application.admin

import com.didit.application.admin.provided.AdminFinder
import com.didit.application.admin.provided.AdminManager
import com.didit.application.admin.required.AdminRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class AdminManagementService(
    private val adminRepository: AdminRepository,
    private val adminFinder: AdminFinder,
) : AdminManager {
    companion object {
        private val logger = LoggerFactory.getLogger(AdminManagementService::class.java)
    }

    @Transactional
    override fun approve(adminId: UUID) {
        val admin = adminFinder.findByIdOrThrow(adminId)

        admin.approve()

        adminRepository.save(admin)

        logger.info("어드민 승인 - adminId: $adminId")
    }

    @Transactional
    override fun reject(adminId: UUID) {
        val admin = adminFinder.findByIdOrThrow(adminId)

        admin.reject()

        adminRepository.save(admin)

        logger.info("어드민 거절 - adminId: $adminId")
    }

    @Transactional
    override fun delete(adminId: UUID) {
        val admin = adminFinder.findByIdOrThrow(adminId)

        adminRepository.delete(admin)

        logger.info("어드민 삭제 - adminId: $adminId")
    }
}
