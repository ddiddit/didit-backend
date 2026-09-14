package com.didit.adapter.webapi.retrospect.dto

import com.didit.adapter.webapi.organization.dto.ProjectListResponse
import com.didit.adapter.webapi.organization.dto.TagListResponse
import com.didit.application.retrospect.dto.RetrospectiveDetailResult
import com.didit.application.retrospect.dto.RetrospectiveResultV2Result
import com.didit.domain.retrospect.RetrospectiveFlowVersion
import com.didit.domain.retrospect.RetrospectiveResultDetail
import com.didit.domain.retrospect.RetrospectiveResultV2
import com.didit.domain.retrospect.RetrospectiveSummary
import java.time.LocalDateTime
import java.util.UUID

data class StructuredRetrospectiveResultResponse(
    val id: UUID,
    val flowVersion: RetrospectiveFlowVersion,
    val title: String?,
    val content: StructuredRetrospectiveContentResponse,
    val completedAt: LocalDateTime?,
    val project: ProjectListResponse?,
    val tags: List<TagListResponse>,
) {
    companion object {
        fun from(result: RetrospectiveDetailResult): StructuredRetrospectiveResultResponse {
            val retrospective = result.retrospective
            return StructuredRetrospectiveResultResponse(
                id = retrospective.id,
                flowVersion = retrospective.flowVersion,
                title = retrospective.title,
                content =
                    if (retrospective.isV2()) {
                        StructuredRetrospectiveContentResponse.fromV2(retrospective.resultV2)
                    } else {
                        StructuredRetrospectiveContentResponse.fromV1(retrospective.summary)
                    },
                completedAt = retrospective.completedAt,
                project = result.project?.let(ProjectListResponse::from),
                tags = result.tags.map(TagListResponse::from),
            )
        }
    }
}

data class StructuredRetrospectiveContentResponse(
    val summary: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val processes: List<String>,
    val learnings: List<String>,
    val insight: StructuredResultDetailResponse,
    val nextActions: List<StructuredResultDetailResponse>,
) {
    companion object {
        fun fromV2Result(result: RetrospectiveResultV2Result) =
            StructuredRetrospectiveContentResponse(
                summary = result.summary ?: StructuredResultDefaults.SUMMARY,
                strengths = result.strengths.orDefault(StructuredResultDefaults.STRENGTH),
                improvements = result.improvements.orDefault(StructuredResultDefaults.IMPROVEMENT),
                processes = result.processes.orDefault(StructuredResultDefaults.PROCESS),
                learnings = result.learnings.orDefault(StructuredResultDefaults.LEARNING),
                insight = result.insight.toResponseOrDefault(StructuredResultDefaults.INSIGHT),
                nextActions =
                    result.nextActions
                        ?.map(StructuredResultDetailResponse::from)
                        .orDefault(StructuredResultDefaults.NEXT_ACTION),
            )

        fun fromV2(result: RetrospectiveResultV2?) =
            StructuredRetrospectiveContentResponse(
                summary = result?.summary ?: StructuredResultDefaults.SUMMARY,
                strengths = result?.strengths.orDefault(StructuredResultDefaults.STRENGTH),
                improvements = result?.improvements.orDefault(StructuredResultDefaults.IMPROVEMENT),
                processes = result?.processes.orDefault(StructuredResultDefaults.PROCESS),
                learnings = result?.learnings.orDefault(StructuredResultDefaults.LEARNING),
                insight = result?.insight.toResponseOrDefault(StructuredResultDefaults.INSIGHT),
                nextActions =
                    result
                        ?.nextActions
                        ?.map(StructuredResultDetailResponse::from)
                        .orDefault(StructuredResultDefaults.NEXT_ACTION),
            )

        fun fromV1(summary: RetrospectiveSummary?) =
            StructuredRetrospectiveContentResponse(
                summary = summary?.summary.normalized() ?: StructuredResultDefaults.SUMMARY,
                strengths = listOf(StructuredResultDefaults.STRENGTH),
                improvements = summary?.blockedPoint.normalizedItemsOrDefault(StructuredResultDefaults.IMPROVEMENT),
                processes = summary?.solutionProcess.normalizedItemsOrDefault(StructuredResultDefaults.PROCESS),
                learnings = summary?.lessonLearned.normalizedItemsOrDefault(StructuredResultDefaults.LEARNING),
                insight =
                    summary
                        ?.toInsight()
                        .toResponseOrDefault(StructuredResultDefaults.INSIGHT),
                nextActions =
                    summary
                        ?.toNextAction()
                        ?.let(StructuredResultDetailResponse::from)
                        ?.let(::listOf)
                        ?: listOf(StructuredResultDefaults.NEXT_ACTION),
            )

        private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)

        private fun <T> List<T>?.orDefault(default: T): List<T> = this?.takeIf { it.isNotEmpty() } ?: listOf(default)

        private fun Iterable<String?>?.normalizedItemsOrDefault(default: String): List<String> =
            this
                ?.mapNotNull { it.normalized() }
                ?.distinct()
                ?.take(2)
                ?.takeIf { it.isNotEmpty() }
                ?: listOf(default)

        private fun RetrospectiveSummary.toInsight(): RetrospectiveResultDetail? = detailOrNull(insightTitle, insightDescription)

        private fun RetrospectiveSummary.toNextAction(): RetrospectiveResultDetail? = detailOrNull(nextActionTitle, nextActionDescription)

        private fun detailOrNull(
            title: String?,
            description: String?,
        ): RetrospectiveResultDetail? {
            val normalizedTitle = title.normalized() ?: return null
            val normalizedDescription = description.normalized() ?: return null
            return RetrospectiveResultDetail(normalizedTitle, normalizedDescription)
        }

        private fun RetrospectiveResultDetail?.toResponseOrDefault(
            default: StructuredResultDetailResponse,
        ): StructuredResultDetailResponse = this?.let(StructuredResultDetailResponse::from) ?: default
    }
}

data class StructuredResultDetailResponse(
    val title: String,
    val description: String,
) {
    companion object {
        fun from(result: RetrospectiveResultDetail) =
            StructuredResultDetailResponse(
                title = result.title,
                description = result.description,
            )
    }
}

object StructuredResultDefaults {
    const val SUMMARY = "충분한 내용이 모이지 않았어요."
    const val STRENGTH = "오늘 잘한 점은 대화에서 확인되지 않았어요."
    const val IMPROVEMENT = "아쉬웠던 점은 대화에서 확인되지 않았어요."
    const val PROCESS = "해결 과정은 대화에서 확인되지 않았어요."
    const val LEARNING = "배운 점은 대화에서 확인되지 않았어요."
    val INSIGHT =
        StructuredResultDetailResponse(
            title = "인사이트를 만들기 어려웠어요.",
            description = "디딧의 인사이트를 만들 만큼 충분한 내용이 없었어요.",
        )
    val NEXT_ACTION =
        StructuredResultDetailResponse(
            title = "다음 행동을 제안하기 어려웠어요.",
            description = "다음에 해볼 일은 대화에서 확인되지 않았어요.",
        )
}
