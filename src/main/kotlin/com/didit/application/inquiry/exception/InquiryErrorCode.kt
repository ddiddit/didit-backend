package com.didit.application.inquiry.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class InquiryErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 문의를 찾을 수 없습니다."),
}
