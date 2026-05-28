package com.didit.adapter.persistence.retrospective

import com.didit.application.organization.required.RetrospectTagRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.Retrospective
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class RetrospectDeletionAdapterTest {
    @Mock
    private lateinit var retrospectiveRepository: RetrospectiveRepository

    @Mock
    private lateinit var retrospectiveTagRepository: RetrospectTagRepository

    @InjectMocks
    private lateinit var adapter: RetrospectDeletionAdapter

    @Test
    fun `deleteByUserId가 호출되면 해당 유저의 모든 회고가 삭제된다`() {
        val userId = UUID.randomUUID()
        val retro1 = Retrospective.create(userId)
        val retro2 = Retrospective.create(userId)
        whenever(retrospectiveRepository.findAllByUserId(userId)).thenReturn(listOf(retro1, retro2))

        adapter.deleteByUserId(userId)

        verify(retrospectiveRepository).delete(retro1)
        verify(retrospectiveRepository).delete(retro2)
    }

    @Test
    fun `deleteByUserId가 호출되면 회고의 태그도 함께 삭제된다`() {
        val userId = UUID.randomUUID()
        val retro1 = Retrospective.create(userId)
        val retro2 = Retrospective.create(userId)
        whenever(retrospectiveRepository.findAllByUserId(userId)).thenReturn(listOf(retro1, retro2))

        adapter.deleteByUserId(userId)

        verify(retrospectiveTagRepository).deleteAllByRetrospectiveId(retro1.id)
        verify(retrospectiveTagRepository).deleteAllByRetrospectiveId(retro2.id)
    }

    @Test
    fun `각 회고마다 태그가 먼저 삭제된 후 회고가 삭제된다`() {
        val userId = UUID.randomUUID()
        val retro = Retrospective.create(userId)
        whenever(retrospectiveRepository.findAllByUserId(userId)).thenReturn(listOf(retro))

        adapter.deleteByUserId(userId)

        val inOrder = inOrder(retrospectiveTagRepository, retrospectiveRepository)
        inOrder.verify(retrospectiveTagRepository).deleteAllByRetrospectiveId(retro.id)
        inOrder.verify(retrospectiveRepository).delete(retro)
    }

    @Test
    fun `회고가 없으면 아무것도 삭제되지 않는다`() {
        val userId = UUID.randomUUID()
        whenever(retrospectiveRepository.findAllByUserId(userId)).thenReturn(emptyList())

        adapter.deleteByUserId(userId)

        verify(retrospectiveTagRepository, never()).deleteAllByRetrospectiveId(any())
        verify(retrospectiveRepository, never()).delete(any())
    }
}
