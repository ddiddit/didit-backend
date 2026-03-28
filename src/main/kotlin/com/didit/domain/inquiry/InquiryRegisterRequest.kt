package com.didit.domain.inquiry

import java.util.UUID

data class InquiryRegisterRequest(
    val userId: UUID,
    val email: String,
    val type: InquiryType,
    val typeEtc: String?,
    val content: String,
    val isAgreed: Boolean,
) {
    init {
        require(isAgreed) { "개인정보 수집 동의는 필수입니다." }

        if (type == InquiryType.ETC) {
            require(!typeEtc.isNullOrBlank()) { "기타 유형은 추가 입력이 필요합니다." }
        }
    }
}
