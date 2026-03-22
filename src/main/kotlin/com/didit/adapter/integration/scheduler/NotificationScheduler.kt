package com.didit.adapter.integration.scheduler

import com.didit.adapter.integration.fcm.FcmClient
import com.didit.application.notification.provided.DeviceTokenFinder
import com.didit.application.notification.provided.NotificationHistoryRegister
import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.domain.notification.NotificationHistoryCreateRequest
import com.didit.domain.notification.NotificationType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalTime

@Component
class NotificationScheduler(
    private val notificationSettingFinder: NotificationSettingFinder,
    private val deviceTokenFinder: DeviceTokenFinder,
    private val fcmClient: FcmClient,
    private val notificationHistoryRegister: NotificationHistoryRegister,
) {
    companion object {
        const val DAILY_REMINDER_TITLE = "회고 작성 알림"
        const val DAILY_REMINDER_BODY = "하루를 마무리하며 오늘의 회고를 기록해 보세요."
    }

    @Scheduled(cron = "0 * * * * *")
    fun sendReminderNotifications() {
        val now = LocalTime.now().withSecond(0).withNano(0)
        val settings = notificationSettingFinder.findAllByReminderTime(now)

        settings.forEach { setting ->
            val tokens = deviceTokenFinder.findAllByUserId(setting.userId)

            if (tokens.isEmpty()) return@forEach

            tokens.forEach { token ->
                fcmClient.sendMessage(
                    token = token.token,
                    title = DAILY_REMINDER_TITLE,
                    body = DAILY_REMINDER_BODY,
                )
            }

            notificationHistoryRegister.save(
                NotificationHistoryCreateRequest(
                    userId = setting.userId,
                    type = NotificationType.DAILY_REMINDER,
                    title = DAILY_REMINDER_TITLE,
                    body = DAILY_REMINDER_BODY,
                ),
            )
        }
    }
}
