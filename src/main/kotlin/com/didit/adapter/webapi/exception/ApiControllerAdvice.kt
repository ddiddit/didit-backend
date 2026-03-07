package com.didit.adapter.webapi.exception

import com.didit.application.common.exception.BusinessException
import com.didit.application.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

@RestControllerAdvice
class ApiControllerAdvice {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(exception: MethodArgumentNotValidException): ProblemDetail {
        val detail =
            exception.bindingResult.fieldErrors
                .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }

        log.warn("[VALIDATION] 요청 값 검증 실패 message={}", detail)

        return ProblemDetail
            .forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                detail,
            ).apply {
                title = HttpStatus.BAD_REQUEST.reasonPhrase
                setProperty("timestamp", OffsetDateTime.now().toString())
                setProperty("code", ErrorCode.INVALID_REQUEST.name)
            }
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ProblemDetail {
        val errorCode = exception.errorCode

        log.warn("[BUSINESS] 비즈니스 예외 발생 code={}, message={}", errorCode.name, exception.message)

        return ProblemDetail
            .forStatusAndDetail(
                errorCode.status,
                errorCode.detail,
            ).apply {
                title = errorCode.status.reasonPhrase
                setProperty("timestamp", OffsetDateTime.now().toString())
                setProperty("code", errorCode.name)
            }
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ProblemDetail {
        log.error("[SYSTEM] 처리되지 않은 예외 발생", exception)

        return ProblemDetail
            .forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.detail,
            ).apply {
                title = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase
                setProperty("timestamp", OffsetDateTime.now().toString())
                setProperty("code", ErrorCode.INTERNAL_SERVER_ERROR.name)
            }
    }
}
