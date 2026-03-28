package com.didit.application.inquiry.provided

import com.didit.adapter.webapi.inquiry.dto.InquiryRequest
import com.didit.domain.inquiry.Inquiry
import java.util.UUID

interface InquiryRegister {
    fun register(
        userId: UUID,
        request: InquiryRequest,
    ): Inquiry
}
