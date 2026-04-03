package com.didit.application.inquiry.exception

import com.didit.application.common.exception.BusinessException

class InquiryNotFoundException : BusinessException(InquiryErrorCode.INQUIRY_NOT_FOUND)
