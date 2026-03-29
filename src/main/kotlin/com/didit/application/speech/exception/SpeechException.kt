package com.didit.application.speech.exception

import com.didit.application.common.exception.BusinessException

class SpeechEmptyFileException : BusinessException(SpeechErrorCode.SPEECH_EMPTY_FILE)

class SpeechUnsupportedFileException(
    originalFilename: String?,
    contentType: String?,
) : BusinessException(
    SpeechErrorCode.SPEECH_UNSUPPORTED_FILE,
    "originalFilename: $originalFilename, contentType: $contentType",
)

class SpeechTranscriptionFailedException(
    message: String,
) : BusinessException(
    SpeechErrorCode.SPEECH_TRANSCRIPTION_FAILED,
    message,
)

class SpeechEmptyResultException : BusinessException(SpeechErrorCode.SPEECH_EMPTY_RESULT)