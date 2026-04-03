package com.didit.adapter.webapi.inquiry.dto

import com.didit.domain.inquiry.InquiryType

data class InquiryRequest(
    val type: InquiryType,
    val typeEtc: String?,
    val content: String,
    val isAgreed: Boolean,
)
