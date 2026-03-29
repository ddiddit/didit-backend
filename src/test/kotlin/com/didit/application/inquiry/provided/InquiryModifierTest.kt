package com.didit.application.inquiry.provided

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InquiryModifierTest {
    @Mock
    lateinit var inquiryModifier: InquiryModifier

    @Test
    fun `answer(inquiryId, adminId, answer)`() {
        val inquiryId = UUID.randomUUID()
        val adminId = UUID.randomUUID()
        val answerText = "답변 내용"

        inquiryModifier.answer(inquiryId, adminId, answerText)

        verify(inquiryModifier).answer(inquiryId, adminId, answerText)
    }

    @Test
    fun `updateAnswer(inquiryId, adminId, answer)`() {
        val inquiryId = UUID.randomUUID()
        val adminId = UUID.randomUUID()
        val answerText = "업데이트 답변"

        inquiryModifier.updateAnswer(inquiryId, adminId, answerText)

        verify(inquiryModifier).updateAnswer(inquiryId, adminId, answerText)
    }

    @Test
    fun `delete(userId)`() {
        val inquiryId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        inquiryModifier.delete(inquiryId, userId)
        verify(inquiryModifier).delete(inquiryId, userId)
    }
}
