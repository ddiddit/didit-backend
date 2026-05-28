package com.didit.adapter.persistence.notification

import com.didit.application.notification.required.NotificationHistoryRepository
import com.didit.application.notification.required.NotificationSettingRepository
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class NotificationDeletionAdapterTest {
    @Mock
    private lateinit var notificationHistoryRepository: NotificationHistoryRepository

    @Mock
    private lateinit var notificationSettingRepository: NotificationSettingRepository

    @InjectMocks
    private lateinit var adapter: NotificationDeletionAdapter

    @Test
    fun `deleteByUserId가 호출되면 notificationHistory와 notificationSetting을 삭제한다`() {
        val userId = UUID.randomUUID()

        adapter.deleteByUserId(userId)

        verify(notificationHistoryRepository).deleteAllByUserId(userId)
        verify(notificationSettingRepository).deleteByUserId(userId)
    }

    @Test
    fun `notificationHistory와 notificationSetting이 순서대로 삭제된다`() {
        val userId = UUID.randomUUID()

        adapter.deleteByUserId(userId)

        val inOrder = inOrder(notificationHistoryRepository, notificationSettingRepository)
        inOrder.verify(notificationHistoryRepository).deleteAllByUserId(userId)
        inOrder.verify(notificationSettingRepository).deleteByUserId(userId)
    }
}
