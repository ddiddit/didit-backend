package com.didit.application.admin

import com.didit.application.admin.provided.AdminUserManager
import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import com.didit.application.auth.provided.UserFinder
import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.required.DeviceTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
@Service
class AdminUserManagementService(
    private val userFinder: UserFinder,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val auditLogger: AuditLogger,
) : AdminUserManager {
    companion object {
        private val logger = LoggerFactory.getLogger(AdminUserManagementService::class.java)
    }

    @Transactional
    override fun forceWithdraw(
        adminId: UUID,
        userId: UUID,
    ) {
        val user = userFinder.findByIdOrThrow(userId)

        user.withdraw()
        userRepository.save(user)

        deviceTokenRepository.deleteByUserId(userId)
        refreshTokenRepository.deleteByUserId(userId)

        logger.info("강제 탈퇴 처리 완료 - adminId: $adminId, userId: $userId")

        auditLogger.log(
            actorId = adminId,
            actorType = ActorType.ADMIN,
            action = AuditAction.USER_FORCE_WITHDREW,
            targetId = userId,
            targetType = "USER",
        )
    }
}
