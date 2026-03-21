package com.didit.application.retrospect.port.`in`

import com.didit.application.retrospect.dto.result.RetrospectiveResult
import java.util.UUID

interface GetRetrospectiveResultUseCase {
    fun getResult(retrospectiveId: UUID): RetrospectiveResult
}