package com.didit.application.organization.provided

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ProjectFinderTest {
    @Mock
    lateinit var projectFinder: ProjectFinder

    @Test
    fun `findAllByUserId`() {
        val userId = UUID.randomUUID()

        projectFinder.findAllByUserId(userId)

        verify(projectFinder).findAllByUserId(userId)
    }
}
