package com.didit.application.retrospect

import com.didit.application.retrospect.required.GeneratedRetrospectiveResultV2
import com.didit.domain.retrospect.RetrospectiveResultDetail
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RetrospectiveResultV2TonePolicyTest {
    private val policy = RetrospectiveResultV2TonePolicy()

    @Test
    fun `사용자에게 보이는 결과 서술을 해요체로 보정한다`() {
        val normalized =
            policy.normalize(
                GeneratedRetrospectiveResultV2(
                    title = "배포 회고",
                    summary = "개발 서버에서 테스트했다.",
                    strengths = listOf("팀원과 문제를 해결하였다", "문제를 해결할 수 있었다.", "직접 적용해봤다."),
                    improvements = listOf("사전 확인이 부족했음", "좋은 경험이었다."),
                    processes = listOf("테스트 범위를 점검함.", "준비 과정이 중요하다."),
                    learnings = listOf("협업할 때 확인이 필요하다."),
                    insight = RetrospectiveResultDetail("협업", "함께 확인하여 마무리했다."),
                    nextActions = listOf(RetrospectiveResultDetail("사전 점검", "다음에는 테스트 범위를 줄인다.")),
                    inputTokens = 10,
                    outputTokens = 5,
                ),
            )

        assertThat(normalized.title).isEqualTo("배포 회고")
        assertThat(normalized.summary).isEqualTo("개발 서버에서 테스트했어요.")
        assertThat(normalized.strengths)
            .containsExactly("팀원과 문제를 해결했어요", "문제를 해결할 수 있었어요.", "직접 적용해봤어요.")
        assertThat(normalized.improvements).containsExactly("사전 확인이 부족했어요", "좋은 경험이었어요.")
        assertThat(normalized.processes).containsExactly("테스트 범위를 점검했어요.", "준비 과정이 중요해요.")
        assertThat(normalized.learnings).containsExactly("협업할 때 확인이 필요해요.")
        assertThat(normalized.insight).isEqualTo(RetrospectiveResultDetail("협업", "함께 확인하여 마무리했어요."))
        assertThat(normalized.nextActions)
            .containsExactly(RetrospectiveResultDetail("사전 점검", "다음에는 테스트 범위를 줄여요."))
        assertThat(normalized.inputTokens).isEqualTo(10)
        assertThat(normalized.outputTokens).isEqualTo(5)
    }

    @Test
    fun `보정 뒤에도 보고서체가 남으면 재생성이 필요하다`() {
        val normalized =
            policy.normalize(
                result(summary = "좋다."),
            )

        assertThat(policy.requiresRegeneration(normalized)).isTrue()
        assertThat(policy.requiresRegeneration(result(summary = "직접 적용해봤어요."))).isFalse()
    }

    private fun result(summary: String) =
        GeneratedRetrospectiveResultV2(
            title = "배포 회고",
            summary = summary,
            strengths = null,
            improvements = null,
            processes = null,
            learnings = null,
            insight = null,
            nextActions = null,
            inputTokens = 10,
            outputTokens = 5,
        )
}
