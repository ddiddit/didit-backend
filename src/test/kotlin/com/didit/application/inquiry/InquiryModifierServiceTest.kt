package com.didit.application.inquiry

import com.didit.application.inquiry.exception.InquiryNotFoundException
import com.didit.application.inquiry.required.InquiryRepository
import com.didit.domain.inquiry.Inquiry
import com.didit.domain.inquiry.InquiryStatus
import com.didit.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InquiryModifierServiceTest {
    @Mock
    lateinit var inquiryRepository: InquiryRepository

    @InjectMocks
    lateinit var inquiryModifierService: InquiryModifierService

    private val inquiryId: UUID = UUID.randomUUID()
    private val adminId: UUID = UUID.randomUUID()
    private val userId: UUID = UUID.randomUUID()

    @Test
    fun `문의 답변 성공`() {
        val inquiry = createInquiry()

        whenever(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId))
            .thenReturn(inquiry)
        whenever(inquiryRepository.save(inquiry))
            .thenReturn(inquiry)

        val result = inquiryModifierService.answer(inquiryId, adminId, "답변입니다")

        verify(inquiryRepository).findByIdAndDeletedAtIsNull(inquiryId)
        verify(inquiryRepository).save(inquiry)

        assertThat(result.status).isEqualTo(InquiryStatus.ANSWERED)
        assertThat(result.adminAnswer).isEqualTo("답변입니다")
        assertThat(result.adminId).isEqualTo(adminId)
    }

    @Test
    fun `문의 답변 실패 - 문의 없음`() {
        whenever(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId))
            .thenReturn(null)

        assertThatThrownBy {
            inquiryModifierService.answer(inquiryId, adminId, "답변")
        }.isInstanceOf(InquiryNotFoundException::class.java)
    }

    @Test
    fun `문의 답변 수정 성공`() {
        val inquiry = createAnsweredInquiry()

        whenever(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId))
            .thenReturn(inquiry)
        whenever(inquiryRepository.save(inquiry))
            .thenReturn(inquiry)

        val result = inquiryModifierService.updateAnswer(inquiryId, adminId, "수정된 답변")

        verify(inquiryRepository).save(inquiry)
        assertThat(result.adminAnswer).isEqualTo("수정된 답변")
    }

    @Test
    fun `문의 답변 수정 실패 - 답변 안된 상태`() {
        val inquiry = createInquiry()
        createAnsweredInquiry()

        whenever(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId))
            .thenReturn(inquiry)

        assertThatThrownBy {
            inquiryModifierService.updateAnswer(inquiryId, adminId, "수정")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("답변이 미완료된 문의는 답변 수정이 불가능합니다.")
    }

    @Test
    fun `문의 답변 삭제 성공`() {
        val inquiry = createAnsweredInquiry()

        whenever(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId))
            .thenReturn(inquiry)
        whenever(inquiryRepository.save(inquiry))
            .thenReturn(inquiry)

        val result = inquiryModifierService.deleteAnswer(inquiryId, adminId)

        verify(inquiryRepository).save(inquiry)

        assertThat(result.status).isEqualTo(InquiryStatus.PENDING)
        assertThat(result.adminAnswer).isNull()
        assertThat(result.adminId).isNull()
    }

    @Test
    fun `문의 삭제 성공`() {
        val inquiry = createInquiry(userId = userId)

        whenever(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId))
            .thenReturn(inquiry)

        inquiryModifierService.delete(inquiryId, userId)

        assertThat(inquiry.deletedAt).isNotNull()
    }

    @Test
    fun `문의 삭제 실패 - 권한 없음`() {
        val inquiry = createInquiry(userId = userId)

        whenever(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId))
            .thenReturn(inquiry)

        assertThatThrownBy {
            inquiryModifierService.delete(inquiryId, UUID.randomUUID())
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("삭제 권한이 없습니다.")
    }

    @Test
    fun `문의 삭제 실패 - 문의 없음`() {
        whenever(inquiryRepository.findByIdAndDeletedAtIsNull(inquiryId))
            .thenReturn(null)

        assertThatThrownBy {
            inquiryModifierService.delete(inquiryId, userId)
        }.isInstanceOf(InquiryNotFoundException::class.java)
    }

    private fun createInquiry(userId: UUID = this.userId): Inquiry {
        val user = UserFixture.create(email = "test@test.com")

        return Inquiry.register(
            request =
                com.didit.domain.inquiry.InquiryRegisterRequest(
                    userId = userId,
                    email = user.email!!,
                    type = com.didit.domain.inquiry.InquiryType.BUG,
                    typeEtc = null,
                    content = "문의 내용",
                    isAgreed = true,
                ),
            userId = userId,
            email = user.email,
        )
    }

    private fun createAnsweredInquiry(): Inquiry {
        val inquiry = createInquiry()
        inquiry.answer(adminId, "기존 답변")
        return inquiry
    }
}
