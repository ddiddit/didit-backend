package com.didit.adapter.integration.scheduler

import com.didit.adapter.integration.fcm.FcmClient
import com.didit.application.notification.provided.DeviceTokenFinder
import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.application.notification.required.DeviceTokenRepository
import com.didit.domain.notification.NotificationHistoryCreateRequest
import com.didit.domain.notification.NotificationType
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

@Component
class NotificationScheduler(
    private val notificationSettingFinder: NotificationSettingFinder,
    private val deviceTokenFinder: DeviceTokenFinder,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val fcmClient: FcmClient,
    private val notificationHistoryRegister: NotificationHistoryRegister,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NotificationScheduler::class.java)

        const val DAILY_REMINDER_TITLE = "회고 작성 알림"
        const val DAILY_REMINDER_BODY = "하루를 마무리하며 오늘의 회고를 기록해 보세요."
    }

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    fun sendReminderNotifications() {
        val now = LocalTime.now(ZoneId.of("Asia/Seoul")).withSecond(0).withNano(0)
        val settings = notificationSettingFinder.findAllByReminderTime(now)

        logger.info("회고 알림 스케줄러 실행 - time: $now, 대상 유저 수: ${settings.size}")

        settings.forEach { setting ->
            runCatching {
                sendToUser(setting.userId)
            }.onFailure { e ->
                logger.error("회고 알림 전송 실패 - userId: ${setting.userId}", e)
            }
        }
    }

    private fun sendToUser(userId: UUID) {
        val tokens = deviceTokenFinder.findAllByUserId(userId)
        if (tokens.isEmpty()) return

        val successCount =
            tokens.count { deviceToken ->
                val isExpired =
                    fcmClient.sendMessage(
                        token = deviceToken.token,
                        title = DAILY_REMINDER_TITLE,
                        body = DAILY_REMINDER_BODY,
                    )

                if (isExpired) {
                    deviceTokenRepository.deleteByToken(deviceToken.token)

                    logger.warn("만료된 FCM 토큰 삭제 - userId: $userId, token: ${deviceToken.token.take(20)}...")
                }

                !isExpired
            }

        if (successCount > 0) {
            notificationHistoryRegister.save(
                NotificationHistoryCreateRequest(
                    userId = userId,
                    type = NotificationType.DAILY_REMINDER,
                    title = DAILY_REMINDER_TITLE,
                    body = DAILY_REMINDER_BODY,
                ),
            )
            logger.info("회고 알림 전송 성공 - userId: $userId, 성공 토큰 수: $successCount")
        }
    }
}
