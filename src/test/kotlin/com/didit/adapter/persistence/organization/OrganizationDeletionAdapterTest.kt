package com.didit.adapter.persistence.organization

import com.didit.application.organization.required.ProjectRepository
import com.didit.application.organization.required.TagRepository
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class OrganizationDeletionAdapterTest {
    @Mock
    private lateinit var projectRepository: ProjectRepository

    @Mock
    private lateinit var tagRepository: TagRepository

    @InjectMocks
    private lateinit var adapter: OrganizationDeletionAdapter

    @Test
    fun `deleteByUserId가 호출되면 project와 tag를 삭제한다`() {
        val userId = UUID.randomUUID()

        adapter.deleteByUserId(userId)

        verify(projectRepository).deleteAllByUserId(userId)
        verify(tagRepository).deleteAllByUserId(userId)
    }

    @Test
    fun `project와 tag가 순서대로 삭제된다`() {
        val userId = UUID.randomUUID()

        adapter.deleteByUserId(userId)

        val inOrder = inOrder(projectRepository, tagRepository)
        inOrder.verify(projectRepository).deleteAllByUserId(userId)
        inOrder.verify(tagRepository).deleteAllByUserId(userId)
    }
}
