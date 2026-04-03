package com.didit.domain.inquiry

import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InquiryTest {
    private val userId = UUID.randomUUID()
    private val adminId = UUID.randomUUID()
    private val email = "test@example.com"

    @Test
    fun `문의 등록 시 개인정보 수집 동의 false인 경우 예외`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                InquiryRegisterRequest(
                    userId = userId,
                    email = email,
                    type = InquiryType.USAGE,
                    typeEtc = null,
                    content = "문의 내용",
                    isAgreed = false,
                )
            }
        assertEquals("개인정보 수집 동의는 필수입니다.", exception.message)
    }

    @Test
    fun `기타 유형 Inquiry 생성 시 typeEtc 없으면 예외`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                InquiryRegisterRequest(
                    userId = userId,
                    email = email,
                    type = InquiryType.ETC,
                    typeEtc = null,
                    content = "문의 내용",
                    isAgreed = true,
                )
            }
        assertEquals("기타 유형은 추가 입력이 필요합니다.", exception.message)
    }

    @Test
    fun `문의 register 성공`() {
        val request =
            InquiryRegisterRequest(
                userId = userId,
                email = email,
                type = InquiryType.USAGE,
                typeEtc = null,
                content = "문의 내용",
                isAgreed = true,
            )

        val inquiry = Inquiry.register(request, userId, email)

        assertEquals(userId, inquiry.userId)
        assertEquals(email, inquiry.email)
        assertEquals("문의 내용", inquiry.content)
        assertEquals(InquiryType.USAGE, inquiry.type)
        assertEquals(InquiryStatus.PENDING, inquiry.status)
        assertNull(inquiry.deletedAt)
    }

    @Test
    fun `삭제 권한 없으면 예외`() {
        val request =
            InquiryRegisterRequest(
                userId = userId,
                email = email,
                type = InquiryType.USAGE,
                typeEtc = null,
                content = "문의 내용",
                isAgreed = true,
            )
        val inquiry = Inquiry.register(request, userId, email)

        val otherUserId = UUID.randomUUID()
        val exception =
            assertThrows<IllegalArgumentException> {
                inquiry.delete(otherUserId)
            }
        assertEquals("삭제 권한이 없습니다.", exception.message)
    }

    @Test
    fun `정상 삭제`() {
        val request =
            InquiryRegisterRequest(
                userId = userId,
                email = email,
                type = InquiryType.USAGE,
                typeEtc = null,
                content = "문의 내용",
                isAgreed = true,
            )
        val inquiry = Inquiry.register(request, userId, email)

        inquiry.delete(userId)

        assertNotNull(inquiry.deletedAt)
    }

    @Test
    fun `답변 등록 정상`() {
        val request =
            InquiryRegisterRequest(
                userId = userId,
                email = email,
                type = InquiryType.USAGE,
                typeEtc = null,
                content = "문의 내용",
                isAgreed = true,
            )
        val inquiry = Inquiry.register(request, userId, email)

        inquiry.answer(adminId, "답변 내용")

        assertEquals(InquiryStatus.ANSWERED, inquiry.status)
        assertEquals(adminId, inquiry.adminId)
        assertEquals("답변 내용", inquiry.adminAnswer)
        assertNotNull(inquiry.answeredAt)
    }

    @Test
    fun `이미 답변된 문의 답변 등록 시 예외`() {
        val request =
            InquiryRegisterRequest(
                userId = userId,
                email = email,
                type = InquiryType.USAGE,
                typeEtc = null,
                content = "문의 내용",
                isAgreed = true,
            )
        val inquiry = Inquiry.register(request, userId, email)
        inquiry.answer(adminId, "첫 답변")

        val exception =
            assertThrows<IllegalArgumentException> {
                inquiry.answer(adminId, "두 번째 답변")
            }
        assertEquals("이미 답변이 완료된 문의입니다.", exception.message)
    }

    @Test
    fun `답변 수정 정상`() {
        val request =
            InquiryRegisterRequest(
                userId = userId,
                email = email,
                type = InquiryType.USAGE,
                typeEtc = null,
                content = "문의 내용",
                isAgreed = true,
            )
        val inquiry = Inquiry.register(request, userId, email)
        inquiry.answer(adminId, "첫 답변")

        inquiry.updateAnswer("수정 답변", adminId)

        assertEquals("수정 답변", inquiry.adminAnswer)
    }

    @Test
    fun `답변 미완료 상태에 답변 수정 시 예외`() {
        val request =
            InquiryRegisterRequest(
                userId = userId,
                email = email,
                type = InquiryType.USAGE,
                typeEtc = null,
                content = "문의 내용",
                isAgreed = true,
            )
        val inquiry = Inquiry.register(request, userId, email)

        val exception =
            assertThrows<IllegalArgumentException> {
                inquiry.updateAnswer("수정 답변", adminId)
            }
        assertEquals("답변이 미완료된 문의는 답변 수정이 불가능합니다.", exception.message)
    }
}
