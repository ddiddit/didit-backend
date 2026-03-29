package com.didit.domain.retrospect

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatMessageTest {
    private fun retrospective() = Retrospective.create(UUID.randomUUID())

    @Test
    fun `userAnswer - 빈 내용이면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            ChatMessage.userAnswer(retrospective(), "", QuestionType.Q1)
        }
    }

    @Test
    fun `skippedAnswer - Q4_DEEP이 아니면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            ChatMessage.skippedAnswer(retrospective(), QuestionType.Q1)
        }
    }

    @Test
    fun `skippedAnswer - Q4_DEEP이면 isSkipped가 true이다`() {
        val message = ChatMessage.skippedAnswer(retrospective(), QuestionType.Q4_DEEP)

        assertTrue(message.isSkipped)
        assertEquals("", message.content)
        assertEquals(Sender.USER, message.sender)
    }

    @Test
    fun `question - sender가 AI이고 isSkipped가 false이다`() {
        val message = ChatMessage.question(retrospective(), "오늘 어떤 일을 하셨나요?", QuestionType.Q1)

        assertEquals(Sender.AI, message.sender)
        assertFalse(message.isSkipped)
    }
}
