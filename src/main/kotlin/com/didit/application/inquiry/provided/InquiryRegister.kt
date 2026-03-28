package com.didit.application.inquiry.provided

import com.didit.application.inquiry.dto.RegisterInquiryCommand
import com.didit.domain.inquiry.Inquiry

interface InquiryRegister {
    fun register(request: RegisterInquiryCommand): Inquiry
}
