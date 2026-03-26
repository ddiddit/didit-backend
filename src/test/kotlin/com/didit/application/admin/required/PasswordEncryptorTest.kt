package com.didit.application.admin.required

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class PasswordEncryptorTest {
    @Mock
    lateinit var passwordEncryptor: PasswordEncryptor

    @Test
    fun `encode`() {
        whenever(passwordEncryptor.encode("password123!")).thenReturn("encoded-password")

        val result = passwordEncryptor.encode("password123!")

        verify(passwordEncryptor).encode("password123!")
        assertThat(result).isEqualTo("encoded-password")
    }

    @Test
    fun `matches - correct password`() {
        whenever(passwordEncryptor.matches("password123!", "encoded-password")).thenReturn(true)

        val result = passwordEncryptor.matches("password123!", "encoded-password")

        verify(passwordEncryptor).matches("password123!", "encoded-password")
        assertThat(result).isTrue()
    }

    @Test
    fun `matches - wrong password`() {
        whenever(passwordEncryptor.matches("wrongpassword", "encoded-password")).thenReturn(false)

        val result = passwordEncryptor.matches("wrongpassword", "encoded-password")

        verify(passwordEncryptor).matches("wrongpassword", "encoded-password")
        assertThat(result).isFalse()
    }
}
