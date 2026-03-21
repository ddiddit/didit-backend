package com.didit.application.retrospect.port.`in`

import com.didit.application.retrospect.dto.command.SubmitAnswerCommand
import com.didit.application.retrospect.dto.result.SubmitAnswerResult

interface SubmitAnswerUseCase {
    fun submitAnswer(command: SubmitAnswerCommand): SubmitAnswerResult
}