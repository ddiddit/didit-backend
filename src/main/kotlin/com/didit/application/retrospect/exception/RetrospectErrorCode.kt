package com.didit.application.retrospect.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class RetrospectErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    RETROSPECTIVE_NOT_FOUND(HttpStatus.NOT_FOUND, "회고를 찾을 수 없습니다."),
    RETROSPECTIVE_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 회고입니다."),
    RETROSPECTIVE_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "진행 중인 회고가 아닙니다."),
    DAILY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "오늘 회고 횟수를 모두 사용했습니다."),
    CANNOT_SKIP_QUESTION(HttpStatus.BAD_REQUEST, "해당 질문은 스킵할 수 없습니다."),
}
