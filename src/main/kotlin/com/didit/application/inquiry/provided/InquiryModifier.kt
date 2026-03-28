package com.didit.application.inquiry.provided

import com.didit.domain.inquiry.Inquiry
import java.util.UUID

interface InquiryModifier {
    fun answer(
        inquiryId: UUID,
        adminId: UUID,
        answer: String,
    ): Inquiry

    fun updateAnswer(
        inquiryId: UUID,
        adminId: UUID,
        answer: String,
    ): Inquiry
}
