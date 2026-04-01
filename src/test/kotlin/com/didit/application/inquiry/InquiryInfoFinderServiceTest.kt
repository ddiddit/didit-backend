package com.didit.application.inquiry

import com.didit.application.auth.provided.UserFinder
import com.didit.domain.auth.User
import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InquiryInfoFinderServiceTest {
    @Mock
    lateinit var userFinder: UserFinder

    @InjectMocks
    lateinit var inquiryInfoFinderService: InquiryInfoFinderService

    private val userId = UUID.randomUUID()

    @Test
    fun `이메일 조회 성공`() {
        val user = createUser(email = "test@test.com")

        whenever(userFinder.findByIdOrThrow(userId))
            .thenReturn(user)

        val result = inquiryInfoFinderService.findEmail(userId)

        verify(userFinder).findByIdOrThrow(userId)
        assertThat(result).isEqualTo("test@test.com")
    }

    @Test
    fun `이메일 null이면 NPE 발생`() {
        val user = createUser(email = null)

        whenever(userFinder.findByIdOrThrow(userId))
            .thenReturn(user)

        org.assertj.core.api.Assertions
            .assertThatThrownBy {
                inquiryInfoFinderService.findEmail(userId)
            }.isInstanceOf(NullPointerException::class.java)
    }

    private fun createUser(email: String?): User = UserFixture.create(email = email)
}
