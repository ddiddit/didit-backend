package com.didit.application.inquiry.provided

import com.didit.domain.inquiry.Inquiry
import com.didit.domain.inquiry.InquiryRegisterRequest
import java.util.UUID

interface InquiryRegister {
    fun register(
        request: InquiryRegisterRequest,
        userId: UUID,
    ): Inquiry
}
