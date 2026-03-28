package com.didit.adapter.webapi.inquiry.dto

import com.didit.domain.inquiry.Inquiry
import com.didit.domain.inquiry.InquiryStatus
import com.didit.domain.inquiry.InquiryType
import java.time.LocalDateTime

data class InquiryListResponse(
    val type: InquiryType,
    val content: String,
    val status: InquiryStatus,
    val adminAnswer: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(inquiry: Inquiry): InquiryListResponse =
            InquiryListResponse(
                type = inquiry.type,
                content = inquiry.content,
                status = inquiry.status,
                adminAnswer = inquiry.adminAnswer,
                createdAt = inquiry.createdAt!!,
            )
    }
}
