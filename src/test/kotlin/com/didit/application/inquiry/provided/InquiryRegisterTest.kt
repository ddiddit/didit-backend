package com.didit.application.inquiry.provided

import com.didit.domain.inquiry.InquiryRegisterRequest
import com.didit.domain.inquiry.InquiryType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InquiryRegisterTest {
    @Mock
    lateinit var inquiryRegister: InquiryRegister

    @Test
    fun `register`() {
        val userId = UUID.randomUUID()
        val request =
            InquiryRegisterRequest(
                userId = userId,
                email = "test@example.com",
                type = InquiryType.USAGE,
                typeEtc = null,
                content = "문의 내용입니다.",
                isAgreed = true,
            )
        inquiryRegister.register(request, userId)
        verify(inquiryRegister).register(request, userId)
    }
}
