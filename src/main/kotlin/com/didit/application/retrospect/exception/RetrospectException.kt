package com.didit.application.retrospect.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class RetrospectiveNotFoundException(
    retrospectiveId: UUID,
) : BusinessException(
        RetrospectErrorCode.RETROSPECTIVE_NOT_FOUND,
        "retrospectiveId: $retrospectiveId",
    )

class RetrospectiveAlreadyCompletedException(
    retrospectiveId: UUID,
) : BusinessException(
        RetrospectErrorCode.RETROSPECTIVE_ALREADY_COMPLETED,
        "retrospectiveId: $retrospectiveId",
    )

class RetrospectiveNotInProgressException(
    retrospectiveId: UUID,
) : BusinessException(
        RetrospectErrorCode.RETROSPECTIVE_NOT_IN_PROGRESS,
        "retrospectiveId: $retrospectiveId",
    )

class DailyLimitExceededException(
    userId: UUID,
) : BusinessException(
        RetrospectErrorCode.DAILY_LIMIT_EXCEEDED,
        "userId: $userId",
    )

class SpeechEmptyFileException : BusinessException(RetrospectErrorCode.SPEECH_EMPTY_FILE)

class SpeechUnsupportedFileException(
    originalFilename: String?,
    contentType: String?,
) : BusinessException(
        RetrospectErrorCode.SPEECH_UNSUPPORTED_FILE,
        "originalFilename: $originalFilename, contentType: $contentType",
    )

class SpeechTranscriptionFailedException(
    message: String,
) : BusinessException(
        RetrospectErrorCode.SPEECH_TRANSCRIPTION_FAILED,
        message,
    )

class SpeechEmptyResultException : BusinessException(RetrospectErrorCode.SPEECH_EMPTY_RESULT)
