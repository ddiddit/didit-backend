package com.didit.application.admin.provided

import com.didit.domain.admin.AdminPosition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminInviteManagerTest {
    @Mock
    lateinit var adminInviteManager: AdminInviteManager

    @Test
    fun `invite`() {
        val invitedBy = UUID.randomUUID()

        adminInviteManager.invite(
            invitedBy = invitedBy,
            email = "invite@didit.com",
            position = AdminPosition.DEVELOPER,
        )

        verify(adminInviteManager).invite(
            invitedBy = invitedBy,
            email = "invite@didit.com",
            position = AdminPosition.DEVELOPER,
        )
    }

    @Test
    fun `register`() {
        val token = UUID.randomUUID()

        adminInviteManager.register(
            token = token,
            email = "invite@didit.com",
            password = "password123!",
        )

        verify(adminInviteManager).register(
            token = token,
            email = "invite@didit.com",
            password = "password123!",
        )
    }
}
