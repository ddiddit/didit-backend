package com.didit.application.common.exception

open class BusinessException(
    val errorCode: BaseErrorCode,
) : RuntimeException(errorCode.detail)
