package com.didit.application.admin.required

import com.didit.support.AdminFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminRepositoryTest {
    @Mock
    lateinit var adminRepository: AdminRepository

    @Test
    fun `save`() {
        val admin = AdminFixture.createAdmin()
        whenever(adminRepository.save(admin)).thenReturn(admin)

        val saved = adminRepository.save(admin)

        verify(adminRepository).save(admin)
        assertThat(saved.email).isEqualTo(admin.email)
    }

    @Test
    fun `findById`() {
        val admin = AdminFixture.createAdmin()
        whenever(adminRepository.findById(admin.id)).thenReturn(admin)

        val found = adminRepository.findById(admin.id)

        verify(adminRepository).findById(admin.id)
        assertThat(found?.id).isEqualTo(admin.id)
    }

    @Test
    fun `findById - not found`() {
        val adminId = UUID.randomUUID()
        whenever(adminRepository.findById(adminId)).thenReturn(null)

        val found = adminRepository.findById(adminId)

        assertThat(found).isNull()
    }

    @Test
    fun `findByEmail`() {
        val admin = AdminFixture.createAdmin()
        whenever(adminRepository.findByEmail(admin.email)).thenReturn(admin)

        val found = adminRepository.findByEmail(admin.email)

        verify(adminRepository).findByEmail(admin.email)
        assertThat(found?.email).isEqualTo(admin.email)
    }

    @Test
    fun `findByEmail - not found`() {
        whenever(adminRepository.findByEmail("unknown@didit.com")).thenReturn(null)

        val found = adminRepository.findByEmail("unknown@didit.com")

        assertThat(found).isNull()
    }

    @Test
    fun `findAll`() {
        val admins = listOf(AdminFixture.createAdmin(), AdminFixture.createAdmin(email = "admin2@didit.com"))
        whenever(adminRepository.findAll()).thenReturn(admins)

        val result = adminRepository.findAll()

        verify(adminRepository).findAll()
        assertThat(result).hasSize(2)
    }

    @Test
    fun `delete`() {
        val admin = AdminFixture.createAdmin()

        adminRepository.delete(admin)

        verify(adminRepository).delete(admin)
    }
}
