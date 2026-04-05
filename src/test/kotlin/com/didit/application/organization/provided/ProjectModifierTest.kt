package com.didit.application.organization.provided

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ProjectModifierTest {
    @Mock
    lateinit var projectModifier: ProjectModifier

    @Test
    fun `updateName`() {
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val newName = "새 프로젝트 이름"

        projectModifier.updateName(userId, projectId, newName)

        verify(projectModifier).updateName(userId, projectId, newName)
    }
}
