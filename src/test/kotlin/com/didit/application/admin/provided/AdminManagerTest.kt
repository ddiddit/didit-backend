package com.didit.application.admin.provided

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminManagerTest {
    @Mock
    lateinit var adminManager: AdminManager

    @Test
    fun `approve`() {
        val adminId = UUID.randomUUID()

        adminManager.approve(adminId)

        verify(adminManager).approve(adminId)
    }

    @Test
    fun `reject`() {
        val adminId = UUID.randomUUID()

        adminManager.reject(adminId)

        verify(adminManager).reject(adminId)
    }

    @Test
    fun `delete`() {
        val adminId = UUID.randomUUID()

        adminManager.delete(adminId)

        verify(adminManager).delete(adminId)
    }
}
