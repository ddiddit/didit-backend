package com.didit.application.inquiry.required

import com.didit.domain.inquiry.Inquiry
import com.didit.domain.inquiry.InquiryStatus
import com.didit.domain.inquiry.InquiryType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InquiryRepositoryTest {
    @Mock
    lateinit var inquiryRepository: InquiryRepository

    @Test
    fun `save`() {
        val inquiry =
            Inquiry(
                id = UUID.randomUUID(),
                userId = UUID.randomUUID(),
                email = "test@example.com",
                type = InquiryType.USAGE,
                typeEtc = null,
                content = "문의 내용",
                isAgreed = true,
                status = InquiryStatus.PENDING,
            )
        whenever(inquiryRepository.save(inquiry)).thenReturn(inquiry)

        val result = inquiryRepository.save(inquiry)

        verify(inquiryRepository).save(inquiry)
        assert(result == inquiry)
    }

    @Test
    fun `findAllByUserId(userId)`() {
        val userId = UUID.randomUUID()
        inquiryRepository.findAllByUserId(userId)
        verify(inquiryRepository).findAllByUserId(userId)
    }

    @Test
    fun `findById(inquiryId)`() {
        val inquiryId = UUID.randomUUID()
        inquiryRepository.findById(inquiryId)
        verify(inquiryRepository).findById(inquiryId)
    }

    @Test
    fun `findAllByOrderByCreatedAtDesc()`() {
        inquiryRepository.findAllByOrderByCreatedAtDesc()
        verify(inquiryRepository).findAllByOrderByCreatedAtDesc()
    }
}
