package com.didit.adapter.webapi.inquiry.dto

import com.didit.domain.inquiry.Inquiry
import com.didit.domain.inquiry.InquiryStatus
import com.didit.domain.inquiry.InquiryType
import java.time.LocalDateTime
import java.util.UUID

data class InquiryAdminListResponse(
    val id: UUID,
    val email: String,
    val type: InquiryType,
    val content: String,
    val status: InquiryStatus,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(inquiry: Inquiry): InquiryAdminListResponse =
            InquiryAdminListResponse(
                id = inquiry.id,
                email = inquiry.email,
                type = inquiry.type,
                content = inquiry.content,
                status = inquiry.status,
                createdAt = inquiry.createdAt!!,
            )
    }
}
