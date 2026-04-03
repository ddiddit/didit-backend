package com.didit.adapter.webapi.retrospect

import com.didit.adapter.webapi.auth.annotation.CurrentUserId
import com.didit.adapter.webapi.auth.annotation.RequireAuth
import com.didit.adapter.webapi.response.SuccessResponse
import com.didit.adapter.webapi.retrospect.dto.CalendarResponse
import com.didit.adapter.webapi.retrospect.dto.CompleteRetrospectiveResponse
import com.didit.adapter.webapi.retrospect.dto.DailyRetrospectiveResponse
import com.didit.adapter.webapi.retrospect.dto.RetrospectiveDetailResponse
import com.didit.adapter.webapi.retrospect.dto.RetrospectiveListItemResponse
import com.didit.adapter.webapi.retrospect.dto.RetrospectiveSearchResponse
import com.didit.adapter.webapi.retrospect.dto.SaveRetrospectiveRequest
import com.didit.adapter.webapi.retrospect.dto.SearchHistoryResponse
import com.didit.adapter.webapi.retrospect.dto.StartRetrospectiveResponse
import com.didit.adapter.webapi.retrospect.dto.SubmitAnswerRequest
import com.didit.adapter.webapi.retrospect.dto.UpdateTitleRequest
import com.didit.application.audit.Audit
import com.didit.application.audit.AuditAction
import com.didit.application.retrospect.dto.DeepQuestionResponse
import com.didit.application.retrospect.dto.SubmitAnswerResponse
import com.didit.application.retrospect.exception.SpeechUnsupportedFileException
import com.didit.application.retrospect.provided.RetrospectiveFinder
import com.didit.application.retrospect.provided.RetrospectiveRegister
import com.didit.application.retrospect.provided.SearchHistoryFinder
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.util.UUID

@RequestMapping("/api/v1/retrospectives")
@RestController
class RetrospectApi(
    private val retrospectiveRegister: RetrospectiveRegister,
    private val retrospectiveFinder: RetrospectiveFinder,
    private val searchHistoryFinder: SearchHistoryFinder,
) {
    @Audit(AuditAction.RETROSPECTIVE_STARTED)
    @RequireAuth
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    fun start(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<StartRetrospectiveResponse> {
        val retrospective = retrospectiveRegister.start(userId)

        return SuccessResponse.of(StartRetrospectiveResponse.from(retrospective))
    }

    @RequireAuth
    @PostMapping("/{retrospectiveId}/answers")
    fun submitAnswer(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @RequestBody request: SubmitAnswerRequest,
    ): SuccessResponse<SubmitAnswerResponse> {
        val result =
            retrospectiveRegister.submitAnswer(
                retrospectiveId = retrospectiveId,
                userId = userId,
                content = request.content,
            )
        return SuccessResponse.of(result)
    }

    @RequireAuth
    @PostMapping("/{retrospectiveId}/answers/voice", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun submitVoiceAnswer(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @RequestPart("file") file: MultipartFile,
    ): SuccessResponse<SubmitAnswerResponse> {
        val result =
            retrospectiveRegister.submitVoiceAnswer(
                retrospectiveId = retrospectiveId,
                userId = userId,
                audioBytes = file.bytes,
                filename =
                    file.originalFilename
                        ?: throw SpeechUnsupportedFileException(null, file.contentType),
            )
        return SuccessResponse.of(result)
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{retrospectiveId}/skip")
    fun skipDeepQuestion(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
    ) {
        retrospectiveRegister.skipDeepQuestion(retrospectiveId, userId)
    }

    @RequireAuth
    @PostMapping("/{retrospectiveId}/complete")
    fun complete(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
    ): SuccessResponse<CompleteRetrospectiveResponse> {
        val result = retrospectiveRegister.complete(retrospectiveId, userId)
        return SuccessResponse.of(CompleteRetrospectiveResponse.from(result))
    }

    @RequireAuth
    @GetMapping("/{retrospectiveId}/deep-question")
    fun getDeepQuestion(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
    ): SuccessResponse<DeepQuestionResponse> {
        val result = retrospectiveFinder.findDeepQuestion(retrospectiveId, userId)
        return SuccessResponse.of(result)
    }

    @RequireAuth
    @PostMapping("/{retrospectiveId}/save")
    fun save(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @RequestBody request: SaveRetrospectiveRequest,
    ): SuccessResponse<RetrospectiveDetailResponse> {
        val retrospective =
            retrospectiveRegister.save(
                retrospectiveId = retrospectiveId,
                userId = userId,
                title = request.title,
            )
        return SuccessResponse.of(RetrospectiveDetailResponse.from(retrospective))
    }

    @Audit(AuditAction.RETROSPECTIVE_RESTARTED, targetType = "RETROSPECTIVE")
    @RequireAuth
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{retrospectiveId}/restart")
    fun restart(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
    ): SuccessResponse<StartRetrospectiveResponse> {
        val retrospective = retrospectiveRegister.restart(retrospectiveId, userId)
        return SuccessResponse.of(StartRetrospectiveResponse.from(retrospective))
    }

    @Audit(AuditAction.RETROSPECTIVE_DELETED, targetType = "RETROSPECTIVE")
    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{retrospectiveId}")
    fun delete(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
    ) {
        retrospectiveRegister.delete(retrospectiveId, userId)
    }

    @RequireAuth
    @GetMapping
    fun findAll(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<List<RetrospectiveListItemResponse>> {
        val retrospectives = retrospectiveFinder.findAllByUserId(userId)
        return SuccessResponse.of(retrospectives.map { RetrospectiveListItemResponse.from(it) })
    }

    @RequireAuth
    @GetMapping("/{retrospectiveId}")
    fun findById(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
    ): SuccessResponse<RetrospectiveDetailResponse> {
        val retrospective = retrospectiveFinder.findById(retrospectiveId, userId)
        return SuccessResponse.of(RetrospectiveDetailResponse.from(retrospective))
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{retrospectiveId}/title")
    fun updateTitle(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @RequestBody request: UpdateTitleRequest,
    ) {
        retrospectiveRegister.updateTitle(retrospectiveId, userId, request.title)
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{retrospectiveId}/exit")
    fun exit(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
    ) {
        retrospectiveRegister.exit(retrospectiveId, userId)
    }

    @Validated
    @RequireAuth
    @GetMapping("/calendar")
    fun getCalendar(
        @CurrentUserId userId: UUID,
        @RequestParam year: Int,
        @RequestParam @Min(1) @Max(12) month: Int,
    ): SuccessResponse<CalendarResponse> {
        val retrospectives = retrospectiveFinder.findByUserIdAndYearMonth(userId, year, month)
        val weeklyRetrospectives = retrospectiveFinder.findByUserIdAndCurrentWeek(userId)
        return SuccessResponse.of(CalendarResponse.of(year, month, retrospectives, weeklyRetrospectives))
    }

    @RequireAuth
    @GetMapping("/calendar/daily")
    fun getDailyRetrospectives(
        @CurrentUserId userId: UUID,
        @RequestParam date: LocalDate,
    ): SuccessResponse<List<DailyRetrospectiveResponse>> {
        val retrospectives = retrospectiveFinder.findByUserIdAndDate(userId, date)
        return SuccessResponse.of(retrospectives.map { DailyRetrospectiveResponse.from(it) })
    }

    @RequireAuth
    @GetMapping("/search")
    fun search(
        @CurrentUserId userId: UUID,
        @RequestParam keyword: String,
    ): SuccessResponse<List<RetrospectiveSearchResponse>> {
        val retrospectives = retrospectiveFinder.searchByTitle(userId, keyword)
        return SuccessResponse.of(retrospectives.map { RetrospectiveSearchResponse.from(it) })
    }

    @RequireAuth
    @GetMapping("/search/info")
    fun searchInfo(
        @CurrentUserId userId: UUID,
    ): SuccessResponse<List<SearchHistoryResponse>> {
        val histories = searchHistoryFinder.findRecent(userId)
        return SuccessResponse.of(histories.map { SearchHistoryResponse.from(it) })
    }

    @RequireAuth
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{retrospectiveId}/assign-project")
    fun assignProject(
        @CurrentUserId userId: UUID,
        @PathVariable retrospectiveId: UUID,
        @RequestParam projectId: UUID,
    ) {
        retrospectiveRegister.assignProject(userId, retrospectiveId, projectId)
    }
}
