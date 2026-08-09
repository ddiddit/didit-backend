package com.didit.domain.retrospect

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class RetrospectiveConversationV2Test {
    @Test
    fun `V2 회고는 활성 대화 상태로 생성된다`() {
        val retrospective = Retrospective.createV2(UUID.randomUUID())

        assertThat(retrospective.flowVersion).isEqualTo(RetrospectiveFlowVersion.V2)
        assertThat(retrospective.conversationStatus).isEqualTo(ConversationStatus.ACTIVE)
        assertThat(retrospective.isConversationActive()).isTrue()
    }

    @Test
    fun `첫 안내 메시지만 제목과 보조 문구를 가진다`() {
        val retrospective = Retrospective.createV2(UUID.randomUUID())

        val intro = ChatMessage.v2Intro(retrospective)
        val conversation = ChatMessage.v2AssistantMessage(retrospective, "진행 결과는 어떠셨나요?")

        assertThat(intro.messageType).isEqualTo(ConversationMessageType.INTRO)
        assertThat(intro.content).isEqualTo("오늘 어떤 일을 하셨나요?")
        assertThat(intro.supportingContent)
            .isEqualTo("오늘 진행한 일 중 하나를 떠올려, 작업 내용과 함께 결과나 상태도 같이 적어보세요.")
        assertThat(conversation.messageType).isEqualTo(ConversationMessageType.CONVERSATION)
        assertThat(conversation.supportingContent).isNull()
    }

    @Test
    fun `관련성이 있는 사용자 메시지만 결과에 포함한다`() {
        val retrospective = Retrospective.createV2(UUID.randomUUID())
        val message = ChatMessage.v2UserMessage(retrospective, "배포 작업을 마쳤습니다.")

        message.classify(MessageRelevance.RETROSPECTIVE)

        assertThat(message.includedInResult).isTrue()
        assertThat(message.relevance).isEqualTo(MessageRelevance.RETROSPECTIVE)
    }

    @Test
    fun `회고와 무관한 사용자 메시지는 결과에서 제외한다`() {
        val retrospective = Retrospective.createV2(UUID.randomUUID())
        val message = ChatMessage.v2UserMessage(retrospective, "점심 메뉴를 추천해줘")

        message.classify(MessageRelevance.OFF_TOPIC)

        assertThat(message.includedInResult).isFalse()
    }

    @Test
    fun `분석 항목 상태는 이전 단계로 되돌아가지 않는다`() {
        val item =
            RetrospectiveAnalysisItem(
                retrospectiveId = UUID.randomUUID(),
                itemType = RetrospectiveItemType.FACT,
                status = RetrospectiveItemStatus.ENOUGH,
                summary = "배포 완료",
            )

        item.update(RetrospectiveItemStatus.PARTIAL, "배포 작업 진행")

        assertThat(item.status).isEqualTo(RetrospectiveItemStatus.ENOUGH)
        assertThat(item.summary).isEqualTo("배포 작업 진행")
    }

    @Test
    fun `실패한 턴은 재시도 횟수를 올리고 처리 중 상태로 전환한다`() {
        val turn =
            RetrospectiveConversationTurn(
                retrospectiveId = UUID.randomUUID(),
                clientMessageId = UUID.randomUUID(),
                userMessageId = UUID.randomUUID(),
                turnNumber = 1,
            )
        turn.fail("AI_RESPONSE_GENERATION_FAILED")

        turn.retry()

        assertThat(turn.status).isEqualTo(ConversationTurnStatus.PROCESSING)
        assertThat(turn.attemptCount).isEqualTo(2)
        assertThat(turn.errorCode).isNull()
    }

    @Test
    fun `V1 회고는 V2 대화 종료를 할 수 없다`() {
        val retrospective = Retrospective.create(UUID.randomUUID())

        assertThrows<IllegalStateException> { retrospective.finishConversation() }
    }
}
