package com.didit.application.organization.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class OrganizationErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    DUPLICATED_PROJECT_NAME(HttpStatus.BAD_REQUEST, "이미 존재하는 프로젝트 이름입니다."),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 프로젝트를 찾을 수 없습니다."),

    DUPLICATED_TAG_NAME(HttpStatus.BAD_REQUEST, "이미 존재하는 태그입니다."),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 태그를 찾을 수 없습니다."),
}
