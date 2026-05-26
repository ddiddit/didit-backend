package com.didit.application.notification

import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.provided.AdminNoticeEmailSender
import com.didit.application.notification.required.EmailSender
import com.didit.domain.notification.AdminNoticeEmailSendRequest
import com.didit.domain.notification.AdminNoticeEmailTargetType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class AdminNoticeEmailService(
    private val userRepository: UserRepository,
    private val emailSender: EmailSender,
    private val auditLogger: AuditLogger,
) : AdminNoticeEmailSender {
    companion object {
        private val logger = LoggerFactory.getLogger(AdminNoticeEmailService::class.java)
    }

    override fun send(request: AdminNoticeEmailSendRequest) {
        val users = when (request.targetType) {
            AdminNoticeEmailTargetType.ALL ->
                userRepository.findAllByDeletedAtIsNullAndEmailIsNotNull()

            AdminNoticeEmailTargetType.SELECTED_USERS ->
                userRepository.findAllByIdInAndDeletedAtIsNullAndEmailIsNotNull(request.userIds)
        }

        var sentCount = 0
        var failedCount = 0

        users.forEach { user ->
            runCatching {
                emailSender.send(
                    to = user.email!!,
                    subject = request.subject,
                    body = request.body,
                )
            }.onSuccess {
                sentCount++
            }.onFailure { e ->
                failedCount++
                logger.warn("어드민 공지 이메일 발송 실패 - adminId: ${request.adminId}, userId: ${user.id}, reason: ${e.message}")
            }
        }

        logger.info("어드민 공지 이메일 발송 완료 - adminId: ${request.adminId}, targetType: ${request.targetType}, targetCount: ${users.size}, sentCount: $sentCount, failedCount: $failedCount")

        auditLogger.log(
            actorId = request.adminId,
            actorType = ActorType.ADMIN,
            action = AuditAction.ADMIN_NOTICE_EMAIL_SENT,
            payload =
                mapOf(
                    "targetType" to request.targetType.name,
                    "targetCount" to users.size,
                    "sentCount" to sentCount,
                    "failedCount" to failedCount,
                ),
        )
    }
}