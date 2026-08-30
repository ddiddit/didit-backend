package com.didit.adapter.webapi.retrospect.dto

import com.didit.application.retrospect.dto.RetrospectiveDetailResult
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveResultV2
import com.didit.domain.retrospect.RetrospectiveSummary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class StructuredRetrospectiveResultResponseTest {
    private val userId = UUID.randomUUID()

    @Test
    fun `V2 빈 항목은 저장값을 바꾸지 않고 일관된 안내 문구로 응답한다`() {
        val retrospective =
            Retrospective.createV2(userId).apply {
                startProgress()
                finishConversation()
                saveV2Result(
                    title = "배포 오류 롤백 회고",
                    result =
                        RetrospectiveResultV2(
                            summary = "배포 오류를 발견하고 롤백했다.",
                            strength = null,
                            improvement = null,
                            process = "로그를 확인했다.",
                            learning = null,
                            insight = null,
                            nextActions = null,
                        ),
                )
            }

        val response = StructuredRetrospectiveResultResponse.from(RetrospectiveDetailResult(retrospective, null, emptyList()))

        assertThat(retrospective.resultV2?.strength).isNull()
        assertThat(response.flowVersion.name).isEqualTo("V2")
        assertThat(response.content.strength).isEqualTo(StructuredResultDefaults.STRENGTH)
        assertThat(response.content.nextActions).containsExactly(StructuredResultDefaults.NEXT_ACTION)
    }

    @Test
    fun `기존 V1 결과를 신규 결과 항목으로 호환 변환한다`() {
        val retrospective =
            Retrospective.create(userId).apply {
                startProgress()
                saveSummary(
                    RetrospectiveSummary(
                        summary = "기존 요약",
                        blockedPoint = listOf("일정이 촉박했다."),
                        solutionProcess = listOf("범위를 줄였다."),
                        lessonLearned = listOf("우선순위 합의가 중요하다."),
                        insightTitle = "범위 조정",
                        insightDescription = "초기에 합의해야 한다.",
                        nextActionTitle = "체크인",
                        nextActionDescription = "착수일에 범위를 확인한다.",
                    ),
                )
                complete("기존 회고")
            }

        val response = StructuredRetrospectiveResultResponse.from(RetrospectiveDetailResult(retrospective, null, emptyList()))

        assertThat(response.flowVersion.name).isEqualTo("V1")
        assertThat(response.content.summary).isEqualTo("기존 요약")
        assertThat(response.content.strength).isEqualTo(StructuredResultDefaults.STRENGTH)
        assertThat(response.content.improvement).isEqualTo("일정이 촉박했다.")
        assertThat(response.content.process).isEqualTo("범위를 줄였다.")
        assertThat(response.content.learning).isEqualTo("우선순위 합의가 중요하다.")
        assertThat(response.content.insight).contains("범위 조정", "초기에 합의해야 한다.")
        assertThat(response.content.nextActions).containsExactly("체크인\n착수일에 범위를 확인한다.")
    }
}
