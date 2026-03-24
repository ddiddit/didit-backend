package com.didit.adapter.retrospect.inbound.web

import com.didit.adapter.retrospect.inbound.web.request.StartRetrospectiveRequest
import com.didit.adapter.retrospect.inbound.web.request.SubmitAnswerRequest
import com.didit.adapter.retrospect.inbound.web.response.RetrospectiveResponse
import com.didit.adapter.retrospect.inbound.web.response.RetrospectiveSummaryResponse
import com.didit.adapter.retrospect.inbound.web.response.StartRetrospectiveResponse
import com.didit.adapter.retrospect.inbound.web.response.SubmitAnswerResponse
import com.didit.application.retrospect.dto.command.StartRetrospectiveCommand
import com.didit.application.retrospect.dto.command.SubmitAnswerCommand
import com.didit.application.retrospect.port.inbound.GetRetrospectiveResultUseCase
import com.didit.application.retrospect.port.inbound.StartRetrospectiveUseCase
import com.didit.application.retrospect.port.inbound.SubmitAnswerUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/retrospectives")
class RetrospectiveController(
    private val startRetrospectiveUseCase: StartRetrospectiveUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val getRetrospectiveResultUseCase: GetRetrospectiveResultUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun start(
        @RequestBody request: StartRetrospectiveRequest,
    ): StartRetrospectiveResponse {
        val result =
            startRetrospectiveUseCase.start(
                StartRetrospectiveCommand(
                    userId = request.userId,
                    projectId = request.projectId,
                    tagIds = request.tagIds,
                ),
            )

        return StartRetrospectiveResponse(
            retrospectiveId = result.retrospectiveId,
            questionType = result.questionType,
            question = result.question,
        )
    }

    @PostMapping("/{retrospectiveId}/answers")
    fun submitAnswer(
        @PathVariable retrospectiveId: UUID,
        @RequestBody request: SubmitAnswerRequest,
    ): SubmitAnswerResponse {
        val result =
            submitAnswerUseCase.submitAnswer(
                SubmitAnswerCommand(
                    retrospectiveId = retrospectiveId,
                    answer = request.answer,
                ),
            )

        return SubmitAnswerResponse(
            completed = result.completed,
            questionType = result.questionType,
            question = result.question,
            summary =
                result.summary?.let {
                    RetrospectiveSummaryResponse(
                        doneWork = it.doneWork,
                        blockedPoint = it.blockedPoint,
                        solutionProcess = it.solutionProcess,
                        lessonLearned = it.lessonLearned,
                        insight = it.insight,
                        improvementDirection = it.improvementDirection,
                    )
                },
        )
    }

    @GetMapping("/{retrospectiveId}")
    fun getResult(
        @PathVariable retrospectiveId: UUID,
    ): RetrospectiveResponse {
        val result = getRetrospectiveResultUseCase.getResult(retrospectiveId)

        return RetrospectiveResponse(
            retrospectiveId = result.retrospectiveId,
            status = result.status,
            summary =
                result.summary?.let {
                    RetrospectiveSummaryResponse(
                        doneWork = it.doneWork,
                        blockedPoint = it.blockedPoint,
                        solutionProcess = it.solutionProcess,
                        lessonLearned = it.lessonLearned,
                        insight = it.insight,
                        improvementDirection = it.improvementDirection,
                    )
                },
        )
    }
}
