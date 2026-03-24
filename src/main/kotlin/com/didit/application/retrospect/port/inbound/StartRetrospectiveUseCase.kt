package com.didit.application.retrospect.port.inbound

import com.didit.application.retrospect.dto.command.StartRetrospectiveCommand
import com.didit.application.retrospect.dto.result.StartRetrospectiveResult

interface StartRetrospectiveUseCase {
    fun start(command: StartRetrospectiveCommand): StartRetrospectiveResult
}
