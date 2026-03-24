package com.didit.application.retrospect.service

import com.didit.adapter.retrospect.out.ai.FakeRetrospectiveAiAdapter
import com.didit.adapter.retrospect.out.persistence.InMemoryRetrospectivePersistenceAdapter
import com.didit.application.retrospect.dto.command.StartRetrospectiveCommand
import com.didit.application.retrospect.dto.command.SubmitAnswerCommand
import com.didit.domain.retrospect.enums.QuestionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class RetrospectiveServiceTest {
    private lateinit var service: RetrospectiveService
    private lateinit var persistenceAdapter: InMemoryRetrospectivePersistenceAdapter

    @BeforeEach
    fun setUp() {
        persistenceAdapter = InMemoryRetrospectivePersistenceAdapter()
        service =
            RetrospectiveService(
                retrospectiveCommandPort = persistenceAdapter,
                retrospectiveQueryPort = persistenceAdapter,
                retrospectiveAiPort = FakeRetrospectiveAiAdapter(),
            )
    }

    @Test
    fun `회고 시작 시 Q1을 반환한다`() {
        val result =
            service.start(
                StartRetrospectiveCommand(
                    userId = UUID.randomUUID(),
                    projectId = null,
                    tagIds = emptyList(),
                ),
            )

        assertEquals(QuestionType.Q1, result.questionType)
        assertNotNull(result.retrospectiveId)
        assertTrue(result.question.isNotBlank())
    }

    @Test
    fun `Q1_Q2_Q3_Q4 답변 후 완료된다`() {
        val startResult =
            service.start(
                StartRetrospectiveCommand(
                    userId = UUID.randomUUID(),
                    projectId = null,
                    tagIds = emptyList(),
                ),
            )

        val q2 =
            service.submitAnswer(
                SubmitAnswerCommand(startResult.retrospectiveId, "오늘 API 설계를 했어요."),
            )
        assertFalse(q2.completed)
        assertEquals(QuestionType.Q2, q2.questionType)

        val q3 =
            service.submitAnswer(
                SubmitAnswerCommand(startResult.retrospectiveId, "도메인 분리가 어려웠어요."),
            )
        assertFalse(q3.completed)
        assertEquals(QuestionType.Q3, q3.questionType)

        val q4 =
            service.submitAnswer(
                SubmitAnswerCommand(startResult.retrospectiveId, "포트와 어댑터를 나누어 해결했어요."),
            )
        assertFalse(q4.completed)
        assertEquals(QuestionType.Q4_DEEP, q4.questionType)

        val done =
            service.submitAnswer(
                SubmitAnswerCommand(startResult.retrospectiveId, "다음에는 더 빨리 구조를 잡고 싶어요."),
            )
        assertTrue(done.completed)
        assertNotNull(done.summary)
    }
}
