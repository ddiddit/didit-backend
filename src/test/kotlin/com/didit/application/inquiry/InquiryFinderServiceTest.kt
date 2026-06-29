package com.didit.application.inquiry

import com.didit.application.inquiry.exception.InquiryErrorCode
import com.didit.application.inquiry.exception.InquiryNotFoundException
import com.didit.application.inquiry.required.InquiryRepository
import com.didit.domain.inquiry.Inquiry
import com.didit.domain.inquiry.InquiryRegisterRequest
import com.didit.domain.inquiry.InquiryType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class InquiryFinderServiceTest {
    @Mock
    lateinit var inquiryRepository: InquiryRepository

    @InjectMocks
    lateinit var inquiryFinderService: InquiryFinderService

    private val userId = UUID.randomUUID()
    private val inquiryId = UUID.randomUUID()

    @Test
    fun `최근 1년 기준 유저 문의 전체 조회 성공`() {
        val inquiries = listOf(createInquiry(), createInquiry())

        whenever(
            inquiryRepository.findAllByUserIdAndDeletedAtIsNullAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(userId),
                any(),
            ),
        ).thenReturn(inquiries)

        val before = LocalDateTime.now().minusYears(1)

        val result = inquiryFinderService.findAll(userId)

        val createdAtCaptor = argumentCaptor<LocalDateTime>()

        verify(inquiryRepository).findAllByUserIdAndDeletedAtIsNullAndCreatedAtAfterOrderByCreatedAtDesc(
            eq(userId),
            createdAtCaptor.capture(),
        )

        val after = LocalDateTime.now().minusYears(1)

        assertThat(createdAtCaptor.firstValue).isBetween(before, after)
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(inquiries)
    }

    @Test
    fun `관리자 문의 전체 조회 성공`() {
        val inquiries = listOf(createInquiry(), createInquiry(), createInquiry())

        whenever(inquiryRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc())
            .thenReturn(inquiries)

        val result = inquiryFinderService.findAllForAdmin()

        verify(inquiryRepository).findAllByDeletedAtIsNullOrderByCreatedAtDesc()
        assertThat(result).hasSize(3)
        assertThat(result).isEqualTo(inquiries)
    }

    @Test
    fun `문의 단건 조회 성공`() {
        val inquiry = createInquiry()

        whenever(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId))
            .thenReturn(inquiry)

        val result = inquiryFinderService.findById(inquiryId)

        verify(inquiryRepository).findByIdAndDeletedAtIsNull(inquiryId)
        assertThat(result).isEqualTo(inquiry)
    }

    @Test
    fun `문의 단건 조회 실패 - 문의 없음`() {
        whenever(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId))
            .thenReturn(null)

        assertThatThrownBy {
            inquiryFinderService.findById(inquiryId)
        }.isInstanceOf(InquiryNotFoundException::class.java)

        verify(inquiryRepository).findByIdAndDeletedAtIsNull(inquiryId)
    }

    @Test
    fun `문의 단건 조회 실패 - 에러코드 검증`() {
        whenever(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId))
            .thenReturn(null)

        assertThatThrownBy {
            inquiryFinderService.findById(inquiryId)
        }.isInstanceOf(InquiryNotFoundException::class.java)
            .extracting { it as InquiryNotFoundException }
            .extracting("errorCode")
            .isEqualTo(InquiryErrorCode.INQUIRY_NOT_FOUND)
    }

    private fun createInquiry(userId: UUID = UUID.randomUUID()): Inquiry {
        val request =
            InquiryRegisterRequest(
                userId = userId,
                email = "test@test.com",
                type = InquiryType.BUG,
                typeEtc = null,
                content = "문의 내용입니다.",
                isAgreed = true,
            )

        return Inquiry.register(
            request = request,
            userId = userId,
            email = request.email,
        )
    }
}
