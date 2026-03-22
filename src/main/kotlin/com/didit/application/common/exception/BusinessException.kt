package com.didit.application.common.exception

open class BusinessException(
    val errorCode: BaseErrorCode,
    message: String = errorCode.detail,
) : RuntimeException(message)
