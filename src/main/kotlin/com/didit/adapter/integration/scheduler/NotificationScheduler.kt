package com.didit.adapter.integration.scheduler

import com.didit.adapter.integration.fcm.FcmClient
import com.didit.application.notification.provided.DeviceTokenFinder
import com.didit.application.notification.provided.NotificationSettingFinder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalTime

@Component
class NotificationScheduler(
    private val notificationSettingFinder: NotificationSettingFinder,
    private val deviceTokenFinder: DeviceTokenFinder,
    private val fcmClient: FcmClient,
) {
    @Scheduled(cron = "0 * * * * *")
    fun sendReminderNotifications() {
        val now = LocalTime.now().withSecond(0).withNano(0)

        val settings = notificationSettingFinder.findAllByReminderTime(now)

        settings.forEach { setting ->
            deviceTokenFinder.findAllByUserId(setting.userId).forEach { token ->
                fcmClient.sendMessage(
                    token = token.token,
                    title = "회고 작성 알림",
                    body = "하루를 마무리하며 오늘의 회고를 기록해 보세요.",
                )
            }
        }
    }
}
