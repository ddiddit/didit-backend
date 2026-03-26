package com.didit.application.admin.provided

import com.didit.application.admin.exception.AdminNotFoundException
import com.didit.support.AdminFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminFinderTest {
    @Mock
    lateinit var adminFinder: AdminFinder

    @Test
    fun `findByIdOrThrow`() {
        val admin = AdminFixture.createAdmin()
        whenever(adminFinder.findByIdOrThrow(admin.id)).thenReturn(admin)

        val found = adminFinder.findByIdOrThrow(admin.id)

        verify(adminFinder).findByIdOrThrow(admin.id)
        assertThat(found.id).isEqualTo(admin.id)
    }

    @Test
    fun `findByIdOrThrow - not found`() {
        val adminId = UUID.randomUUID()
        whenever(adminFinder.findByIdOrThrow(adminId)).thenThrow(AdminNotFoundException())

        assertThatThrownBy { adminFinder.findByIdOrThrow(adminId) }
            .isInstanceOf(AdminNotFoundException::class.java)
    }

    @Test
    fun `findAll`() {
        val admins = listOf(AdminFixture.createAdmin(), AdminFixture.createAdmin(email = "admin2@didit.com"))
        whenever(adminFinder.findAll()).thenReturn(admins)

        val result = adminFinder.findAll()

        verify(adminFinder).findAll()
        assertThat(result).hasSize(2)
    }
}
