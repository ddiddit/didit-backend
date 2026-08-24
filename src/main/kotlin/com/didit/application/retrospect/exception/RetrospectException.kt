package com.didit.application.retrospect.exception

import com.didit.application.common.exception.BusinessException
import java.util.UUID

class RetrospectiveNotFoundException(
    retrospectiveId: UUID,
) : BusinessException(
        RetrospectErrorCode.RETROSPECTIVE_NOT_FOUND,
        "retrospectiveId: $retrospectiveId",
    )

class SummaryNotGeneratedException(
    retrospectiveId: UUID,
) : BusinessException(
        RetrospectErrorCode.SUMMARY_NOT_GENERATED,
        "retrospectiveId: $retrospectiveId",
    )

class SummaryGenerationInProgressException(
    retrospectiveId: UUID,
) : BusinessException(
        RetrospectErrorCode.SUMMARY_GENERATION_IN_PROGRESS,
        "retrospectiveId: $retrospectiveId",
    )

class SummaryAlreadyGeneratedException(
    retrospectiveId: UUID,
) : BusinessException(
        RetrospectErrorCode.SUMMARY_ALREADY_GENERATED,
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
    cause: Throwable? = null,
) : BusinessException(
        RetrospectErrorCode.SPEECH_TRANSCRIPTION_FAILED,
        message,
    ) {
    init {
        if (cause != null) initCause(cause)
    }
}

class SpeechEmptyResultException : BusinessException(RetrospectErrorCode.SPEECH_EMPTY_RESULT)

class RetrospectiveFlowVersionMismatchException(
    retrospectiveId: UUID,
) : BusinessException(RetrospectErrorCode.RETROSPECTIVE_FLOW_VERSION_MISMATCH, "retrospectiveId: $retrospectiveId")

class ConversationAlreadyFinishedException(
    retrospectiveId: UUID,
) : BusinessException(RetrospectErrorCode.CONVERSATION_ALREADY_FINISHED, "retrospectiveId: $retrospectiveId")

class ConversationTurnInProgressException(
    retrospectiveId: UUID,
) : BusinessException(RetrospectErrorCode.TURN_IN_PROGRESS, "retrospectiveId: $retrospectiveId")

class AnotherConversationTurnInProgressException(
    retrospectiveId: UUID,
) : BusinessException(RetrospectErrorCode.ANOTHER_TURN_IN_PROGRESS, "retrospectiveId: $retrospectiveId")

class DuplicateMessageContentMismatchException(
    clientMessageId: UUID,
) : BusinessException(RetrospectErrorCode.DUPLICATE_MESSAGE_CONTENT_MISMATCH, "clientMessageId: $clientMessageId")

class ConversationAiFailedException(
    retrospectiveId: UUID,
    cause: Throwable? = null,
) : BusinessException(RetrospectErrorCode.CONVERSATION_AI_FAILED, "retrospectiveId: $retrospectiveId") {
    init {
        if (cause != null) initCause(cause)
    }
}
