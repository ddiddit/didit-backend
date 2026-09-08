package com.didit.application.retrospect.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class RetrospectErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    RETROSPECTIVE_FLOW_VERSION_MISMATCH(HttpStatus.BAD_REQUEST, "요청한 회고 플로우와 일치하지 않습니다."),
    CONVERSATION_ALREADY_FINISHED(HttpStatus.CONFLICT, "이미 종료된 회고 대화입니다."),
    TURN_IN_PROGRESS(HttpStatus.CONFLICT, "동일한 메시지를 처리하고 있습니다."),
    ANOTHER_TURN_IN_PROGRESS(HttpStatus.CONFLICT, "이전 메시지의 응답을 생성하고 있습니다."),
    DUPLICATE_MESSAGE_CONTENT_MISMATCH(HttpStatus.CONFLICT, "동일한 메시지 ID에 다른 내용을 사용할 수 없습니다."),
    CONVERSATION_AI_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AI 응답 생성에 실패했습니다. 다시 시도해주세요."),
    RESULT_GENERATION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "회고 결과 생성에 실패했습니다. 다시 시도해주세요."),

    RETROSPECTIVE_NOT_FOUND(HttpStatus.NOT_FOUND, "회고를 찾을 수 없습니다."),
    RETROSPECTIVE_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 회고입니다."),
    RETROSPECTIVE_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "진행 중인 회고가 아닙니다."),
    DAILY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "오늘 회고 횟수를 모두 사용했습니다."),
    SUMMARY_NOT_GENERATED(HttpStatus.BAD_REQUEST, "AI 요약이 아직 생성되지 않았습니다."),
    SUMMARY_GENERATION_IN_PROGRESS(HttpStatus.CONFLICT, "AI 요약을 생성 중입니다."),
    SUMMARY_ALREADY_GENERATED(HttpStatus.CONFLICT, "AI 요약이 이미 생성되었습니다."),
    RETROSPECTIVE_MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "메모를 찾을 수 없습니다."),
    DUPLICATE_RETROSPECTIVE_MEMO(HttpStatus.CONFLICT, "오늘은 이미 이 회고의 메모를 작성했습니다."),
    INVALID_RETROSPECTIVE_MEMO_CONTENT(HttpStatus.BAD_REQUEST, "메모 내용은 비어 있을 수 없습니다."),

    SPEECH_EMPTY_FILE(HttpStatus.BAD_REQUEST, "음성 파일이 비어 있습니다."),
    SPEECH_UNSUPPORTED_FILE(HttpStatus.BAD_REQUEST, "지원하지 않는 음성 파일 형식입니다."),
    SPEECH_EMPTY_RESULT(HttpStatus.BAD_REQUEST, "음성 인식 결과가 비어 있습니다."),
    SPEECH_TRANSCRIPTION_FAILED(HttpStatus.BAD_REQUEST, "음성 인식에 실패했습니다."),
}
