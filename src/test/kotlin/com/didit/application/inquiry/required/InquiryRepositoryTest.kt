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
import kotlin.test.assertEquals

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
        assertEquals(inquiry, result)
    }

    @Test
    fun `findAllByUserIdAndDeletedAtIsNull`() {
        val userId = UUID.randomUUID()

        inquiryRepository.findAllByUserIdAndDeletedAtIsNull(userId)

        verify(inquiryRepository).findAllByUserIdAndDeletedAtIsNull(userId)
    }

    @Test
    fun `findByIdAndDeletedAtIsNull`() {
        val inquiryId = UUID.randomUUID()

        inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId)

        verify(inquiryRepository).findByIdAndDeletedAtIsNull(inquiryId)
    }

    @Test
    fun `findAllByDeletedAtIsNullOrderByCreatedAtDesc`() {
        inquiryRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc()

        verify(inquiryRepository).findAllByDeletedAtIsNullOrderByCreatedAtDesc()
    }
}
