package com.didit.application.admin

import com.didit.application.admin.provided.AdminPromptPreviewAnalysisItem
import com.didit.application.admin.provided.AdminPromptPreviewCommand
import com.didit.application.admin.provided.AdminPromptPreviewMessage
import com.didit.application.admin.provided.AdminPromptPreviewState
import com.didit.application.admin.provided.AdminPromptSource
import com.didit.application.admin.required.AdminPromptPreviewAIClient
import com.didit.application.common.exception.BusinessException
import com.didit.application.prompt.required.PromptRepository
import com.didit.application.retrospect.AttachmentSensitiveDataDetector
import com.didit.application.retrospect.ConversationAnswerGuidePolicy
import com.didit.application.retrospect.ConversationV2TurnPolicy
import com.didit.application.retrospect.required.ConversationAnalysisUpdate
import com.didit.application.retrospect.required.GeneratedConversationTurn
import com.didit.domain.auth.UserExperience
import com.didit.domain.prompt.Prompt
import com.didit.domain.prompt.PromptJobType
import com.didit.domain.prompt.PromptType
import com.didit.domain.retrospect.ConversationAnswerGuideMode
import com.didit.domain.retrospect.ConversationMessageType
import com.didit.domain.retrospect.ConversationTurnAction
import com.didit.domain.retrospect.ConversationUserIntent
import com.didit.domain.retrospect.MessageRelevance
import com.didit.domain.retrospect.RetrospectiveItemStatus
import com.didit.domain.retrospect.RetrospectiveItemType
import com.didit.domain.retrospect.Sender
import com.didit.domain.shared.Job
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.UUID

class AdminPromptPreviewServiceTest {
    private val promptRepository = mock<PromptRepository>()
    private val aiClient = mock<AdminPromptPreviewAIClient>()
    private val service =
        AdminPromptPreviewService(
            promptRepository,
            aiClient,
            ConversationV2TurnPolicy(),
            ConversationAnswerGuidePolicy(AttachmentSensitiveDataDetector()),
        )

    @BeforeEach
    fun setUp() {
        whenever(aiClient.preview(any(), any())).thenReturn(generatedResult())
    }

    @Test
    fun `저장된 V2 프롬프트로 첫 preview를 실행한다`() {
        whenever(promptRepository.findByJobTypeAndPromptType(PromptJobType.DEVELOPER, PromptType.CONVERSATION_V2))
            .thenReturn(Prompt(jobType = PromptJobType.DEVELOPER, promptType = PromptType.CONVERSATION_V2, content = "저장 템플릿"))
        val requestCaptor = argumentCaptor<com.didit.application.retrospect.required.ConversationTurnAIRequest>()

        val result = service.preview(command(promptSource = AdminPromptSource.SAVED))

        verify(aiClient).preview(org.mockito.kotlin.eq("저장 템플릿"), requestCaptor.capture())
        assertThat(requestCaptor.firstValue.job).isEqualTo(Job.DEVELOPER)
        assertThat(result.assistantMessage!!.content).isEqualTo("확인했어요.\n다음은 무엇인가요?")
        verify(promptRepository, never()).save(any())
    }

    @Test
    fun `초안은 저장하지 않고 전달된 템플릿으로 실행한다`() {
        val result = service.preview(command(promptSource = AdminPromptSource.DRAFT, draftPrompt = "저장 전 초안"))

        verify(aiClient).preview(org.mockito.kotlin.eq("저장 전 초안"), any())
        assertThat(result.nextState.messages).anyMatch { it.content == "첫 업무 회고" }
        verify(promptRepository, never()).findByJobTypeAndPromptType(any(), any())
        verify(promptRepository, never()).save(any())
    }

    @Test
    fun `첫 턴은 intro와 일곱 EMPTY 분석 항목에서 시작한다`() {
        val requestCaptor = argumentCaptor<com.didit.application.retrospect.required.ConversationTurnAIRequest>()

        val result = service.preview(command(promptSource = AdminPromptSource.DRAFT, draftPrompt = "초안"))

        verify(aiClient).preview(any(), requestCaptor.capture())
        assertThat(requestCaptor.firstValue.messages).hasSize(2)
        assertThat(
            requestCaptor.firstValue.messages
                .first()
                .content,
        ).isEqualTo("오늘 어떤 일을 하셨나요?")
        assertThat(requestCaptor.firstValue.analysisItems)
            .extracting<com.didit.domain.retrospect.RetrospectiveItemStatus> { it.status }
            .containsOnly(RetrospectiveItemStatus.EMPTY)
        assertThat(
            result.nextState.messages
                .first()
                .messageType,
        ).isEqualTo(ConversationMessageType.INTRO)
        assertThat(result.nextState.analysisItems).hasSize(7)
    }

    @Test
    fun `후속 턴은 이전 상태를 보존하고 관련 분석 항목을 갱신한다`() {
        val first = service.preview(command(promptSource = AdminPromptSource.DRAFT, draftPrompt = "초안"))
        whenever(aiClient.preview(any(), any())).thenAnswer { invocation ->
            val request = invocation.getArgument<com.didit.application.retrospect.required.ConversationTurnAIRequest>(1)
            generatedTurn(
                relevance = MessageRelevance.RETROSPECTIVE,
                updates = listOf(update(RetrospectiveItemType.PROCESS, request.currentMessageId, RetrospectiveItemStatus.ENOUGH)),
            )
        }

        val second = service.preview(command(priorState = first.nextState, message = "후속 업무 회고"))

        assertThat(second.nextState.messages)
            .extracting<String> { it.content }
            .containsSubsequence("오늘 어떤 일을 하셨나요?", "첫 업무 회고", "확인했어요.\n다음은 무엇인가요?", "후속 업무 회고")
        assertThat(
            second.nextState.analysisItems
                .single { it.itemType == RetrospectiveItemType.PROCESS }
                .status,
        ).isEqualTo(RetrospectiveItemStatus.ENOUGH)
    }

    @Test
    fun `후속 preview는 운영 대화 정책 상태를 AI 요청과 최종 응답에 적용한다`() {
        val requestCaptor = argumentCaptor<com.didit.application.retrospect.required.ConversationTurnAIRequest>()
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(message(UUID.randomUUID())),
                analysisItems =
                    RetrospectiveItemType.entries.map {
                        AdminPromptPreviewAnalysisItem(it, RetrospectiveItemStatus.EMPTY, null)
                    },
                recentQuestionTargets = listOf(RetrospectiveItemType.FACT),
                completedQuestionCount = 6,
                completionPreviouslyOffered = false,
            )

        val result = service.preview(command(priorState = priorState))

        verify(aiClient).preview(any(), requestCaptor.capture())
        assertThat(requestCaptor.firstValue.recentQuestionTargets).containsExactly(RetrospectiveItemType.FACT)
        assertThat(requestCaptor.firstValue.eligibleQuestionTargets).contains(RetrospectiveItemType.FACT)
        assertThat(requestCaptor.firstValue.completionRecommended).isFalse()
        assertThat(requestCaptor.firstValue.completionPreviouslyOffered).isFalse()
        assertThat(result.assistantMessage!!.content)
            .isEqualTo("확인했어요.\n다음은 무엇인가요?")
        assertThat(result.nextState.completedQuestionCount).isEqualTo(7)
        assertThat(result.nextState.completionPreviouslyOffered).isFalse()
    }

    @Test
    fun `종료 제안 뒤 계속하기 preview는 AI 호출 없이 추가 회고를 시작한다`() {
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(message(UUID.randomUUID())),
                analysisItems =
                    RetrospectiveItemType.entries.map {
                        AdminPromptPreviewAnalysisItem(it, RetrospectiveItemStatus.EMPTY, null)
                    },
                recentQuestionTargets = listOf(RetrospectiveItemType.FACT),
                completedQuestionCount = 6,
                completionPreviouslyOffered = true,
            )

        val result =
            service.preview(
                command(
                    priorState = priorState,
                    conversationIntent = ConversationUserIntent.CONTINUE_AFTER_COMPLETION,
                ),
            )

        verify(aiClient, org.mockito.kotlin.never()).preview(any(), any())
        assertThat(result.assistantMessage!!.content).isEqualTo("좋아요. 이어서 말씀해주세요.")
        assertThat(result.nextState.conversationMode)
            .isEqualTo(com.didit.domain.retrospect.RetrospectiveConversationMode.EXTENDING)
        assertThat(result.nextState.extensionRoundCount).isEqualTo(1)
    }

    @Test
    fun `추가 회고 preview는 질문을 제거하고 무근거 턴을 기록한다`() {
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(message(UUID.randomUUID())),
                analysisItems =
                    RetrospectiveItemType.entries.map {
                        AdminPromptPreviewAnalysisItem(
                            it,
                            RetrospectiveItemStatus.EMPTY,
                            null,
                        )
                    },
                completionPreviouslyOffered = true,
                conversationMode = com.didit.domain.retrospect.RetrospectiveConversationMode.EXTENDING,
                extensionRoundCount = 1,
            )

        val result = service.preview(command(priorState = priorState, message = "네"))

        assertThat(result.nextState.currentQuestionTarget).isNull()
        assertThat(result.nextState.extensionConsecutiveEmptyCount).isEqualTo(1)
        assertThat(result.assistantMessage!!.content).isEqualTo("알겠어요. 이어서 말씀해주시면 함께 정리해볼게요.")
    }

    @Test
    fun `거부한 직전 질문 항목은 후속 preview 질문 대상에서 제외한다`() {
        val requestCaptor = argumentCaptor<com.didit.application.retrospect.required.ConversationTurnAIRequest>()
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(message(UUID.randomUUID())),
                analysisItems =
                    RetrospectiveItemType.entries.map {
                        AdminPromptPreviewAnalysisItem(it, RetrospectiveItemStatus.EMPTY, null)
                    },
                recentQuestionTargets = listOf(RetrospectiveItemType.FACT),
                consecutiveAskCount = 1,
            )
        whenever(aiClient.preview(any(), any())).thenReturn(
            generatedTurn().copy(
                action = ConversationTurnAction.REFLECT,
                question = null,
                questionTarget = null,
                declinedItemTypes = listOf(RetrospectiveItemType.FACT),
            ),
        )

        val first = service.preview(command(priorState = priorState))
        service.preview(command(priorState = first.nextState, message = "다른 이야기"))

        verify(aiClient, org.mockito.kotlin.times(2)).preview(any(), requestCaptor.capture())
        assertThat(
            first.nextState.analysisItems
                .single { it.itemType == RetrospectiveItemType.FACT }
                .questionAllowed,
        ).isFalse()
        assertThat(requestCaptor.secondValue.eligibleQuestionTargets).doesNotContain(RetrospectiveItemType.FACT)
    }

    @Test
    fun `preview도 과거에 질문한 항목을 다시 질문 후보에 넣지 않는다`() {
        val requestCaptor = argumentCaptor<com.didit.application.retrospect.required.ConversationTurnAIRequest>()
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(message(UUID.randomUUID())),
                analysisItems =
                    RetrospectiveItemType.entries.map { type ->
                        AdminPromptPreviewAnalysisItem(
                            itemType = type,
                            status =
                                if (type ==
                                    RetrospectiveItemType.PROCESS
                                ) {
                                    RetrospectiveItemStatus.PARTIAL
                                } else {
                                    RetrospectiveItemStatus.ENOUGH
                                },
                            summary = "$type 요약",
                        )
                    },
                askedQuestionTargets = setOf(RetrospectiveItemType.PROCESS),
            )

        service.preview(command(priorState = priorState, message = "추가 답변"))

        verify(aiClient).preview(any(), requestCaptor.capture())
        assertThat(requestCaptor.firstValue.eligibleQuestionTargets).doesNotContain(RetrospectiveItemType.PROCESS)
    }

    @Test
    fun `preview도 모든 항목이 스킵 완료되면 종료를 제안한다`() {
        // Break caught: admin preview diverges from production and reflects forever after all targets were asked.
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(message(UUID.randomUUID())),
                analysisItems =
                    RetrospectiveItemType.entries.map { type ->
                        AdminPromptPreviewAnalysisItem(
                            itemType = type,
                            status =
                                if (type == RetrospectiveItemType.FACT) {
                                    RetrospectiveItemStatus.ENOUGH
                                } else {
                                    RetrospectiveItemStatus.EMPTY
                                },
                            summary = if (type == RetrospectiveItemType.FACT) "CORS 공부" else null,
                            questionAllowed = false,
                        )
                    },
                askedQuestionTargets = RetrospectiveItemType.entries.toSet(),
                completedQuestionCount = RetrospectiveItemType.entries.size,
            )
        whenever(aiClient.preview(any(), any())).thenAnswer { invocation ->
            val request = invocation.getArgument<com.didit.application.retrospect.required.ConversationTurnAIRequest>(1)
            generatedTurn(
                relevance = MessageRelevance.RETROSPECTIVE,
                updates = listOf(update(RetrospectiveItemType.LEARN, request.currentMessageId, RetrospectiveItemStatus.PARTIAL)),
            ).copy(
                action = ConversationTurnAction.REFLECT,
                question = null,
                questionTarget = null,
            )
        }

        val result =
            service.preview(
                command(
                    priorState = priorState,
                    message = "CORS가 Origin을 기준으로 교차 출처 요청을 제어한다는 걸 배웠어",
                ),
            )

        assertThat(result.nextState.currentQuestionTarget).isNull()
        assertThat(result.nextState.completionPreviouslyOffered).isTrue()
        assertThat(result.assistantMessage!!.content).contains("현재 내용만으로는 회고 결과를 만들기 어려워요")
    }

    @Test
    fun `preview는 과거 메시지만 근거로 재요약한 분석으로 질문을 재개하지 않는다`() {
        val historicalMessageId = UUID.randomUUID()
        val currentMessageId = UUID.randomUUID()
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(message(historicalMessageId)),
                analysisItems =
                    RetrospectiveItemType.entries.map { type ->
                        AdminPromptPreviewAnalysisItem(
                            itemType = type,
                            status =
                                if (type ==
                                    RetrospectiveItemType.FACT
                                ) {
                                    RetrospectiveItemStatus.ENOUGH
                                } else {
                                    RetrospectiveItemStatus.EMPTY
                                },
                            summary = if (type == RetrospectiveItemType.FACT) "CORS 공부" else null,
                            questionAllowed = false,
                        )
                    },
                askedQuestionTargets = RetrospectiveItemType.entries.toSet(),
                completedQuestionCount = RetrospectiveItemType.entries.size,
            )
        whenever(aiClient.preview(any(), any())).thenReturn(
            generatedTurn(
                relevance = MessageRelevance.RETROSPECTIVE,
                updates = listOf(update(RetrospectiveItemType.LEARN, historicalMessageId, RetrospectiveItemStatus.PARTIAL)),
            ).copy(
                action = ConversationTurnAction.REFLECT,
                question = null,
                questionTarget = null,
            ),
        )

        val result =
            service.preview(
                command(
                    priorState = priorState,
                    userMessageId = currentMessageId,
                    message = "맞아",
                ),
            )

        assertThat(result.nextState.currentQuestionTarget).isNull()
    }

    @Test
    fun `모델이 거절 항목을 누락해도 없음 응답은 preview에서 직전 질문을 건너뛴다`() {
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(message(UUID.randomUUID())),
                analysisItems =
                    RetrospectiveItemType.entries.map {
                        AdminPromptPreviewAnalysisItem(it, RetrospectiveItemStatus.EMPTY, null)
                    },
                recentQuestionTargets = listOf(RetrospectiveItemType.BLOCK),
                consecutiveAskCount = 1,
            )
        whenever(aiClient.preview(any(), any())).thenAnswer { invocation ->
            val request = invocation.getArgument<com.didit.application.retrospect.required.ConversationTurnAIRequest>(1)
            generatedTurn(
                updates = listOf(update(RetrospectiveItemType.BLOCK, request.currentMessageId, RetrospectiveItemStatus.PARTIAL)),
            ).copy(
                action = ConversationTurnAction.CONFIRM_COMPLETION,
                acknowledgement = "회고를 마칠까요?",
                question = null,
                questionTarget = null,
                declinedItemTypes = emptyList(),
            )
        }

        val result = service.preview(command(priorState = priorState, message = "없는 것 같아"))

        assertThat(
            result.nextState.analysisItems
                .single { it.itemType == RetrospectiveItemType.BLOCK }
                .questionAllowed,
        ).isFalse()
        assertThat(
            result.nextState.analysisItems
                .single { it.itemType == RetrospectiveItemType.BLOCK }
                .status,
        ).isEqualTo(RetrospectiveItemStatus.EMPTY)
        assertThat(result.nextState.recentQuestionTargets.last()).isNotEqualTo(RetrospectiveItemType.BLOCK)
        assertThat(result.assistantMessage!!.content).doesNotContain("마칠까요")
    }

    @Test
    fun `preview에서도 항목을 연속 거부하면 다른 항목 질문을 이어간다`() {
        val priorState =
            AdminPromptPreviewState(
                messages =
                    listOf(
                        message(UUID.randomUUID()).copy(content = "없어"),
                        message(UUID.randomUUID()).copy(content = "없는 것 같아"),
                    ),
                analysisItems =
                    RetrospectiveItemType.entries.map {
                        AdminPromptPreviewAnalysisItem(it, RetrospectiveItemStatus.EMPTY, null)
                    },
                currentQuestionTarget = RetrospectiveItemType.BLOCK,
            )

        val result = service.preview(command(priorState = priorState, message = "딱히 없어"))

        assertThat(result.assistantMessage!!.content).doesNotContain("회고 결과를 만들기 어려워요")
        assertThat(result.nextState.currentQuestionTarget).isNotNull()
    }

    @Test
    fun `결과를 만들 수 있는 preview에서 종료 의사를 밝히면 AI 말풍선을 반환하지 않는다`() {
        val collectedTypes =
            setOf(
                RetrospectiveItemType.FACT,
                RetrospectiveItemType.PROCESS,
                RetrospectiveItemType.LEARN,
            )
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(message(UUID.randomUUID()), message(UUID.randomUUID())),
                analysisItems =
                    RetrospectiveItemType.entries.map { type ->
                        AdminPromptPreviewAnalysisItem(
                            itemType = type,
                            status = if (type in collectedTypes) RetrospectiveItemStatus.PARTIAL else RetrospectiveItemStatus.EMPTY,
                            summary = type.takeIf { it in collectedTypes }?.let { "$it 요약" },
                        )
                    },
            )

        val result = service.preview(command(priorState = priorState, message = "끝내자"))

        assertThat(result.assistantMessage).isNull()
        assertThat(
            result.nextState.messages
                .last()
                .sender,
        ).isEqualTo(Sender.USER)
    }

    @Test
    fun `채워진 분석 항목 수를 진행도로 반환한다`() {
        whenever(aiClient.preview(any(), any())).thenAnswer { invocation ->
            val request = invocation.getArgument<com.didit.application.retrospect.required.ConversationTurnAIRequest>(1)
            generatedTurn(
                updates = listOf(update(RetrospectiveItemType.FACT, request.currentMessageId, RetrospectiveItemStatus.PARTIAL)),
            )
        }

        val result = service.preview(command(promptSource = AdminPromptSource.DRAFT, draftPrompt = "초안"))

        assertThat(result.progress.filledCount).isEqualTo(1)
        assertThat(result.progress.totalCount).isEqualTo(7)
    }

    @Test
    fun `preview도 운영과 같은 답변 예시 표시 정책을 적용한다`() {
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(message(UUID.randomUUID())),
                analysisItems =
                    RetrospectiveItemType.entries.map { type ->
                        AdminPromptPreviewAnalysisItem(
                            itemType = type,
                            status =
                                if (type ==
                                    RetrospectiveItemType.FACT
                                ) {
                                    RetrospectiveItemStatus.PARTIAL
                                } else {
                                    RetrospectiveItemStatus.EMPTY
                                },
                            summary = if (type == RetrospectiveItemType.FACT) "배포 준비" else null,
                        )
                    },
            )
        whenever(aiClient.preview(any(), any())).thenReturn(
            generatedTurn().copy(
                question = "진행하면서 어려웠던 점은 무엇이었나요?",
                questionTarget = RetrospectiveItemType.BLOCK,
                answerExamples = listOf("테스트 범위가 예상보다 넓었어요", "개발 서버 설정에 시간이 걸렸어요"),
            ),
        )

        val result = service.preview(command(priorState = priorState))

        assertThat(result.assistantMessage!!.answerGuide?.mode).isEqualTo(ConversationAnswerGuideMode.ON_DEMAND)
        assertThat(result.assistantMessage!!.answerGuide?.examples).hasSize(2)
    }

    @Test
    fun `preview도 두 번 연속 질문 뒤 유효한 답변에는 다음 회고 질문을 제공한다`() {
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(message(UUID.randomUUID())),
                analysisItems =
                    RetrospectiveItemType.entries.map {
                        AdminPromptPreviewAnalysisItem(it, RetrospectiveItemStatus.EMPTY, null)
                    },
                consecutiveAskCount = 2,
            )
        whenever(aiClient.preview(any(), any())).thenAnswer { invocation ->
            val request = invocation.getArgument<com.didit.application.retrospect.required.ConversationTurnAIRequest>(1)
            generatedTurn(
                updates = listOf(update(RetrospectiveItemType.PROCESS, request.currentMessageId, RetrospectiveItemStatus.PARTIAL)),
            )
        }

        val result = service.preview(command(priorState = priorState))

        assertThat(result.assistantMessage!!.content).doesNotContain("더 남기고", "더 이어가고", "자유롭게")
        assertThat(result.nextState.consecutiveAskCount).isEqualTo(3)
    }

    @Test
    fun `비회고 응답의 분석 갱신은 무시한다`() {
        whenever(aiClient.preview(any(), any())).thenAnswer { invocation ->
            val request = invocation.getArgument<com.didit.application.retrospect.required.ConversationTurnAIRequest>(1)
            generatedTurn(
                relevance = MessageRelevance.OFF_TOPIC,
                updates = listOf(update(RetrospectiveItemType.FACT, request.currentMessageId, RetrospectiveItemStatus.ENOUGH)),
            )
        }

        val result = service.preview(command(promptSource = AdminPromptSource.DRAFT, draftPrompt = "초안"))

        assertThat(result.nextState.analysisItems).allMatch { it.status == RetrospectiveItemStatus.EMPTY }
    }

    @Test
    fun `내용이 없는 AI 응답은 안전한 인정 문구로 보완한다`() {
        whenever(aiClient.preview(any(), any())).thenReturn(
            generatedTurn(relevance = MessageRelevance.BRIDGEABLE).copy(
                action = ConversationTurnAction.REFLECT,
                acknowledgement = " ",
                interpretation = "",
                question = "",
                questionTarget = null,
            ),
        )

        val result = service.preview(command())

        assertThat(result.assistantMessage!!.content).isEqualTo("알겠어요.")
        assertThat(
            result.nextState.messages
                .last()
                .content,
        ).isEqualTo("알겠어요.")
    }

    @Test
    fun `운영 설정의 context 문자 제한을 preview에도 적용한다`() {
        val requestCaptor = argumentCaptor<com.didit.application.retrospect.required.ConversationTurnAIRequest>()
        val oldMessage = message(UUID.randomUUID()).copy(content = "12345678")
        val priorState =
            AdminPromptPreviewState(
                messages = listOf(oldMessage),
                analysisItems =
                    RetrospectiveItemType.entries.map {
                        AdminPromptPreviewAnalysisItem(it, RetrospectiveItemStatus.EMPTY, null)
                    },
            )

        ApplicationContextRunner()
            .withPropertyValues("retrospective.v2.max-context-characters=10")
            .withBean(PromptRepository::class.java, { promptRepository })
            .withBean(AdminPromptPreviewAIClient::class.java, { aiClient })
            .withBean(ConversationV2TurnPolicy::class.java)
            .withBean(AttachmentSensitiveDataDetector::class.java)
            .withBean(ConversationAnswerGuidePolicy::class.java)
            .withBean(AdminPromptPreviewService::class.java)
            .run { context ->
                context.getBean(AdminPromptPreviewService::class.java).preview(
                    command(priorState = priorState, message = "1234"),
                )
            }

        verify(aiClient).preview(any(), requestCaptor.capture())
        assertThat(requestCaptor.firstValue.messages)
            .extracting<String> { it.content }
            .containsExactly("1234")
    }

    @Test
    fun `잘못된 초안 메시지 또는 상태는 AI 호출 전에 거절한다`() {
        val duplicateId = UUID.randomUUID()
        val invalidState =
            AdminPromptPreviewState(
                messages = listOf(message(id = duplicateId), message(id = duplicateId)),
                analysisItems =
                    RetrospectiveItemType.entries.map {
                        AdminPromptPreviewAnalysisItem(
                            it,
                            RetrospectiveItemStatus.EMPTY,
                            null,
                        )
                    },
            )

        assertThatThrownBy { service.preview(command(promptSource = AdminPromptSource.DRAFT, draftPrompt = " ")) }
            .isInstanceOf(BusinessException::class.java)
        assertThatThrownBy { service.preview(command(message = "  ")) }
            .isInstanceOf(BusinessException::class.java)
        assertThatThrownBy { service.preview(command(priorState = invalidState)) }
            .isInstanceOf(BusinessException::class.java)
        verify(aiClient, never()).preview(any(), any())
    }

    private fun command(
        promptSource: AdminPromptSource = AdminPromptSource.DRAFT,
        draftPrompt: String? = "초안",
        priorState: AdminPromptPreviewState? = null,
        message: String = "첫 업무 회고",
        conversationIntent: ConversationUserIntent = ConversationUserIntent.NORMAL,
        userMessageId: UUID = UUID.randomUUID(),
    ) = AdminPromptPreviewCommand(
        job = Job.DEVELOPER,
        experience = UserExperience.YEARS_1_TO_2,
        promptSource = promptSource,
        draftPrompt = draftPrompt,
        priorState = priorState,
        userMessageId = userMessageId,
        message = message,
        conversationIntent = conversationIntent,
    )

    private fun message(id: UUID) =
        AdminPromptPreviewMessage(
            id = id,
            sender = Sender.USER,
            content = "기존 메시지",
            messageType = ConversationMessageType.CONVERSATION,
            relevance = MessageRelevance.RETROSPECTIVE,
        )

    private fun generatedResult() = generatedTurn()

    private fun generatedTurn(
        relevance: MessageRelevance = MessageRelevance.RETROSPECTIVE,
        updates: List<ConversationAnalysisUpdate> = emptyList(),
    ) = GeneratedConversationTurn(
        action = ConversationTurnAction.ASK,
        acknowledgement = "확인했어요.",
        interpretation = "",
        question = "다음은 무엇인가요?",
        questionTarget = RetrospectiveItemType.FACT,
        relevance = relevance,
        analysisUpdates = updates,
        inputTokens = 10,
        outputTokens = 5,
    )

    private fun update(
        itemType: RetrospectiveItemType,
        messageId: UUID,
        status: RetrospectiveItemStatus,
    ) = ConversationAnalysisUpdate(itemType, status, "$itemType 요약", listOf(messageId))
}
