package com.didit.adapter.integration.scheduler

import com.didit.application.achievement.provided.AchievementDeletionPort
import com.didit.application.auth.required.RefreshTokenRepository
import com.didit.application.auth.required.UserRepository
import com.didit.application.notification.provided.NotificationDeletionPort
import com.didit.application.organization.provided.OrganizationDeletionPort
import com.didit.application.retrospect.provided.RetrospectDeletionPort
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.support.UserFixture
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class CleanupExecutorTest {
    @Mock
    lateinit var retrospectiveRepository: RetrospectiveRepository

    @Mock
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var projectDeletionPort: OrganizationDeletionPort

    @Mock
    lateinit var notificationDeletionPort: NotificationDeletionPort

    @Mock
    lateinit var retrospectDeletionPort: RetrospectDeletionPort

    @Mock
    lateinit var achievementDeletionPort: AchievementDeletionPort

    @InjectMocks
    lateinit var cleanupExecutor: CleanupExecutor

    @Test
    fun `deleteUser - 관련 데이터가 순서대로 삭제된다`() {
        val user =
            UserFixture.create().apply {
                withdraw()
                anonymize()
            }
        whenever(userRepository.findById(user.id)).thenReturn(user)

        cleanupExecutor.deleteUser(user.id)

        val inOrder =
            inOrder(
                projectDeletionPort,
                notificationDeletionPort,
                retrospectDeletionPort,
                achievementDeletionPort,
                userRepository,
            )

        inOrder.verify(projectDeletionPort).deleteByUserId(user.id)
        inOrder.verify(notificationDeletionPort).deleteByUserId(user.id)
        inOrder.verify(retrospectDeletionPort).deleteByUserId(user.id)
        inOrder.verify(achievementDeletionPort).deleteByUserId(user.id)
        inOrder.verify(userRepository).delete(user)
    }

    @Test
    fun `anonymize - 유저를 익명화하고 저장한다`() {
        val user = UserFixture.create().apply { withdraw() }
        whenever(userRepository.findById(user.id)).thenReturn(user)

        cleanupExecutor.anonymize(user.id)

        verify(userRepository).save(user)
    }
}
