package com.didit.adapter.integration.scheduler

import com.didit.application.notification.provided.NotificationSettingFinder
import com.didit.domain.shared.ServiceTime
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalTime

@Component
class NotificationScheduler(
    private val notificationSettingFinder: NotificationSettingFinder,
    private val reminderNotificationSender: ReminderNotificationSender,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(NotificationScheduler::class.java)

        const val DAILY_REMINDER_TITLE = "회고 작성 알림"
        const val DAILY_REMINDER_BODY = "하루를 마무리하며 오늘의 회고를 기록해 보세요."
        const val DAILY_REMINDER_LINK = "/"
    }

    @Scheduled(cron = "0 */10 * * * *")
    fun sendReminderNotifications() {
        val now = LocalTime.now(ServiceTime.ZONE).withSecond(0).withNano(0)
        val settings = notificationSettingFinder.findAllByReminderTime(now)

        logger.info("회고 알림 스케줄러 실행 - time: $now, 대상 유저 수: ${settings.size}")

        settings.forEach { setting ->
            runCatching { reminderNotificationSender.send(setting.userId) }
                .onFailure { e -> logger.error("회고 알림 전송 실패 - userId: ${setting.userId}", e) }
        }
    }
}
