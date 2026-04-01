package com.didit.application.inquiry

import com.didit.application.auth.provided.UserFinder
import com.didit.application.inquiry.required.InquiryRepository
import com.didit.domain.inquiry.Inquiry
import com.didit.domain.inquiry.InquiryRegisterRequest
import com.didit.domain.inquiry.InquiryType
import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InquiryRegisterServiceTest {
    @Mock
    lateinit var inquiryRepository: InquiryRepository

    @Mock
    lateinit var userFinder: UserFinder

    @InjectMocks
    lateinit var inquiryRegisterService: InquiryRegisterService

    private val userId = UUID.randomUUID()

    @Test
    fun `문의 등록 성공`() {
        val user = UserFixture.create(email = "test@test.com")
        val request = createRequest()

        whenever(userFinder.findByIdOrThrow(userId))
            .thenReturn(user)
        whenever(inquiryRepository.save(any()))
            .thenAnswer { it.arguments[0] as Inquiry }

        val result = inquiryRegisterService.register(request, userId)

        verify(userFinder).findByIdOrThrow(userId)
        verify(inquiryRepository).save(any())

        assertThat(result.userId).isEqualTo(userId)
        assertThat(result.email).isEqualTo("test@test.com")
        assertThat(result.content).isEqualTo(request.content)
        assertThat(result.type).isEqualTo(request.type)
    }

    @Test
    fun `문의 등록 실패 - 유저 이메일 없음`() {
        val user = UserFixture.create(email = null)
        val request = createRequest()

        whenever(userFinder.findByIdOrThrow(userId))
            .thenReturn(user)

        assertThatThrownBy {
            inquiryRegisterService.register(request, userId)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("유저 이메일은 null일 수 없습니다.")
    }

    private fun createRequest() =
        InquiryRegisterRequest(
            userId = userId,
            email = "dummy@test.com",
            type = InquiryType.BUG,
            typeEtc = null,
            content = "문의 내용입니다.",
            isAgreed = true,
        )
}
