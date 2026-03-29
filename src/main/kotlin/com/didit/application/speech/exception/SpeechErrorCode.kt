package com.didit.application.speech.exception

import com.didit.application.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class SpeechErrorCode(
    override val status: HttpStatus,
    override val detail: String,
) : BaseErrorCode {
    SPEECH_EMPTY_FILE(HttpStatus.BAD_REQUEST, "음성 파일이 비어 있습니다."),
    SPEECH_UNSUPPORTED_FILE(HttpStatus.BAD_REQUEST, "지원하지 않는 음성 파일 형식입니다."),
    SPEECH_TRANSCRIPTION_FAILED(HttpStatus.BAD_REQUEST, "음성 인식에 실패했습니다."),
    SPEECH_EMPTY_RESULT(HttpStatus.BAD_REQUEST, "음성 인식 결과가 비어 있습니다."),
}
