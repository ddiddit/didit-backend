package com.didit.application.retrospect.port.`in`

import com.didit.application.retrospect.dto.command.StartRetrospectiveCommand
import com.didit.application.retrospect.dto.result.StartRetrospectiveResult

interface StartRetrospectiveUseCase {
    fun start(command: StartRetrospectiveCommand): StartRetrospectiveResult
}