package com.didit.application.admin.required

import com.didit.support.AdminInviteFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminInviteRepositoryTest {
    @Mock
    lateinit var adminInviteRepository: AdminInviteRepository

    @Test
    fun `save`() {
        val invite = AdminInviteFixture.create()
        whenever(adminInviteRepository.save(invite)).thenReturn(invite)

        val saved = adminInviteRepository.save(invite)

        verify(adminInviteRepository).save(invite)
        assertThat(saved.email).isEqualTo(invite.email)
    }

    @Test
    fun `findByToken`() {
        val invite = AdminInviteFixture.create()
        whenever(adminInviteRepository.findByToken(invite.token)).thenReturn(invite)

        val found = adminInviteRepository.findByToken(invite.token)

        verify(adminInviteRepository).findByToken(invite.token)
        assertThat(found?.token).isEqualTo(invite.token)
    }

    @Test
    fun `findByToken - not found`() {
        val token = UUID.randomUUID()
        whenever(adminInviteRepository.findByToken(token)).thenReturn(null)

        val found = adminInviteRepository.findByToken(token)

        assertThat(found).isNull()
    }

    @Test
    fun `existsByEmailAndUsedAtIsNull - exists`() {
        whenever(adminInviteRepository.existsByEmailAndUsedAtIsNull("invite@didit.com")).thenReturn(true)

        val exists = adminInviteRepository.existsByEmailAndUsedAtIsNull("invite@didit.com")

        verify(adminInviteRepository).existsByEmailAndUsedAtIsNull("invite@didit.com")
        assertThat(exists).isTrue()
    }

    @Test
    fun `existsByEmailAndUsedAtIsNull - not exists`() {
        whenever(adminInviteRepository.existsByEmailAndUsedAtIsNull("unknown@didit.com")).thenReturn(false)

        val exists = adminInviteRepository.existsByEmailAndUsedAtIsNull("unknown@didit.com")

        assertThat(exists).isFalse()
    }
}
