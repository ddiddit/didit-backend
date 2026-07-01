package com.didit.application.achievement.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class MissionErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    USER_LEVEL_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "사용자의 레벨 정보를 찾을 수 없습니다."),
    CURRENT_MISSION_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "사용자의 현재 미션을 찾을 수 없습니다."),
    MISSION_DEFINITION_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "해당 미션 정의가 없습니다."),
    USER_MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "미션을 찾을 수 없습니다."),
    INVALID_POPUP_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 팝업 타입입니다."),
}
