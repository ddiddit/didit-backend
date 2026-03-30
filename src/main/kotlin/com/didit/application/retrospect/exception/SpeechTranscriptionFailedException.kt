package com.didit.application.retrospect.exception

import com.didit.application.common.exception.BusinessException
import com.didit.application.common.exception.ErrorCode

class SpeechTranscriptionFailedException(
    message: String,
) : BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, message)
