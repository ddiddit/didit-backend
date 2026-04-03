package com.didit.adapter.webapi.inquiry.dto

import com.didit.domain.inquiry.Inquiry
import com.didit.domain.inquiry.InquiryStatus
import com.didit.domain.inquiry.InquiryType
import java.time.LocalDateTime
import java.util.UUID

data class InquiryResponse(
    val id: UUID,
    val email: String,
    val type: InquiryType,
    val typeEtc: String?,
    val content: String,
    val status: InquiryStatus,
    val createdAt: LocalDateTime?,
    val adminAnswer: String?,
    val answeredAt: LocalDateTime?,
) {
    companion object {
        fun of(inquiry: Inquiry) =
            InquiryResponse(
                id = inquiry.id,
                email = inquiry.email,
                type = inquiry.type,
                typeEtc = inquiry.typeEtc,
                content = inquiry.content,
                status = inquiry.status,
                createdAt = inquiry.createdAt,
                adminAnswer = inquiry.adminAnswer,
                answeredAt = inquiry.answeredAt,
            )
    }
}
