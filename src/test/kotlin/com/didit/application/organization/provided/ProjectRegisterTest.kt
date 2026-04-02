package com.didit.application.organization.provided

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ProjectRegisterTest {
    @Mock
    lateinit var projectRegister: ProjectRegister

    @Test
    fun `create`() {
        val userId = UUID.randomUUID()
        val name = "프로젝트 이름"

        projectRegister.create(userId, name)

        verify(projectRegister).create(userId, name)
    }
}
