package com.didit.application.notification

import com.didit.application.audit.ActorType
import com.didit.application.audit.AuditAction
import com.didit.application.audit.AuditLogger
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.provided.AdminNoticePushSender
import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.application.notification.provided.UserPushSender
import com.didit.domain.notification.AdminNoticePushSendRequest
import com.didit.domain.notification.AdminNoticePushTargetType
import com.didit.domain.notification.NotificationHistoryCreateRequest
import com.didit.domain.notification.NotificationType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AdminNoticePushService(
    private val userRepository: UserRepository,
    private val userPushSender: UserPushSender,
    private val notificationHistoryRegister: NotificationHistoryRegister,
    private val auditLogger: AuditLogger,
) : AdminNoticePushSender {
    companion object {
        private val logger = LoggerFactory.getLogger(AdminNoticePushService::class.java)
    }

    override fun send(request: AdminNoticePushSendRequest) {
        val users =
            when (request.targetType) {
                AdminNoticePushTargetType.ALL ->
                    userRepository.findAllMarketingAgreed()

                AdminNoticePushTargetType.SELECTED_USERS ->
                    userRepository.findAllMarketingAgreedByIdIn(request.userIds)
            }

        var sentCount = 0
        var failedCount = 0

        users.forEach { user ->
            runCatching {
                userPushSender.sendToUser(
                    userId = user.id,
                    title = request.title,
                    body = request.body,
                    link = request.link,
                )
                notificationHistoryRegister.save(
                    NotificationHistoryCreateRequest(
                        userId = user.id,
                        type = NotificationType.ADMIN_MARKETING,
                        title = request.title,
                        body = request.body,
                        link = request.link,
                    ),
                )
            }.onSuccess {
                sentCount++
            }.onFailure { e ->
                failedCount++
                logger.warn(
                    "관리자 마케팅 푸시 발송 실패 - adminId: ${request.adminId}, userId: ${user.id}, reason: ${e.message}",
                )
            }
        }

        logger.info(
            "관리자 마케팅 푸시 발송 완료 - adminId: ${request.adminId}, targetType: ${request.targetType}, " +
                "targetCount: ${users.size}, sentCount: $sentCount, failedCount: $failedCount",
        )

        auditLogger.log(
            actorId = request.adminId,
            actorType = ActorType.ADMIN,
            action = AuditAction.ADMIN_NOTIFICATION_SENT,
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
