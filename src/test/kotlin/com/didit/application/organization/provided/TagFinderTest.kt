package com.didit.application.organization.provided

import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class TagFinderTest {
    @Mock
    lateinit var tagFinder: TagFinder

    @Test
    fun `findAllByUserId 호출`() {
        val userId = UUID.randomUUID()

        tagFinder.findAllByUserId(userId)

        verify(tagFinder).findAllByUserId(userId)
    }
}
