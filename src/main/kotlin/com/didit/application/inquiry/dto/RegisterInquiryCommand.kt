package com.didit.application.inquiry.dto

import com.didit.domain.inquiry.InquiryType
import java.util.UUID

data class RegisterInquiryCommand(
    val userId: UUID,
    val type: InquiryType,
    val typeEtc: String?,
    val content: String,
    val isAgreed: Boolean,
)
