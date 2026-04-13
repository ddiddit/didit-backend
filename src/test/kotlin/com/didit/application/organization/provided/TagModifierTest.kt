package com.didit.application.organization.provided

import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class TagModifierTest {
    @Mock
    lateinit var tagModifier: TagModifier

    @Test
    fun `delete 호출`() {
        val userId = UUID.randomUUID()
        val tagId = UUID.randomUUID()

        tagModifier.delete(userId, tagId)

        verify(tagModifier).delete(userId, tagId)
    }
}
