package com.didit.application.project.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class ProjectErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    DUPLICATED_PROJECT_NAME(HttpStatus.BAD_REQUEST, "이미 존재하는 프로젝트 이름입니다."),
}
