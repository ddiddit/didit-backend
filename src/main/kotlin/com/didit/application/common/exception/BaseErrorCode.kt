package com.didit.application.common.exception

import org.springframework.http.HttpStatus

interface BaseErrorCode {
    val status: HttpStatus
    val detail: String
    val name: String
}
