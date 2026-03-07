package com.didit.application.common.exception

open class BusinessException(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.detail)
