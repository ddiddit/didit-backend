package com.didit.application.retrospect.provided

import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.domain.retrospect.Retrospective
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RetrospectiveFinderTest {
    @Mock
    lateinit var retrospectiveFinder: RetrospectiveFinder

    private val userId = UUID.randomUUID()
    private val retrospectiveId = UUID.randomUUID()

    @Test
    fun `findById - 회고를 반환한다`() {
        val retro = Retrospective.create(userId)
        whenever(retrospectiveFinder.findById(retrospectiveId, userId)).thenReturn(retro)

        val found = retrospectiveFinder.findById(retrospectiveId, userId)

        verify(retrospectiveFinder).findById(retrospectiveId, userId)
        assertThat(found.userId).isEqualTo(userId)
    }

    @Test
    fun `findById - 존재하지 않으면 예외가 발생한다`() {
        whenever(retrospectiveFinder.findById(retrospectiveId, userId))
            .thenThrow(RetrospectiveNotFoundException(retrospectiveId))

        assertThatThrownBy { retrospectiveFinder.findById(retrospectiveId, userId) }
            .isInstanceOf(RetrospectiveNotFoundException::class.java)
    }

    @Test
    fun `findAllByUserId - 유저의 회고 목록을 반환한다`() {
        val retros = listOf(Retrospective.create(userId), Retrospective.create(userId))
        whenever(retrospectiveFinder.findAllByUserId(userId)).thenReturn(retros)

        val found = retrospectiveFinder.findAllByUserId(userId)

        verify(retrospectiveFinder).findAllByUserId(userId)
        assertThat(found).hasSize(2)
    }

    @Test
    fun `countByUserIdAndDate - 오늘 회고 횟수를 반환한다`() {
        whenever(retrospectiveFinder.countByUserIdAndDate(userId, LocalDate.now())).thenReturn(2)

        val count = retrospectiveFinder.countByUserIdAndDate(userId, LocalDate.now())

        verify(retrospectiveFinder).countByUserIdAndDate(userId, LocalDate.now())
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `findRecentByUserId - 최근 회고 목록을 반환한다`() {
        val retros = listOf(Retrospective.create(userId), Retrospective.create(userId))
        whenever(retrospectiveFinder.findRecentByUserId(userId, 5)).thenReturn(retros)

        val found = retrospectiveFinder.findRecentByUserId(userId, 5)

        verify(retrospectiveFinder).findRecentByUserId(userId, 5)
        assertThat(found).hasSize(2)
    }

    @Test
    fun `findLatestCompletedByUserId - 가장 최근 완료된 회고를 반환한다`() {
        val retro = Retrospective.create(userId)
        whenever(retrospectiveFinder.findLatestCompletedByUserId(userId)).thenReturn(retro)

        val found = retrospectiveFinder.findLatestCompletedByUserId(userId)

        verify(retrospectiveFinder).findLatestCompletedByUserId(userId)
        assertThat(found).isNotNull
    }

    @Test
    fun `findLatestCompletedByUserId - 완료된 회고가 없으면 null을 반환한다`() {
        whenever(retrospectiveFinder.findLatestCompletedByUserId(userId)).thenReturn(null)

        val found = retrospectiveFinder.findLatestCompletedByUserId(userId)

        verify(retrospectiveFinder).findLatestCompletedByUserId(userId)
        assertThat(found).isNull()
    }

    @Test
    fun `searchByTitle - 키워드에 해당하는 회고 목록을 반환한다`() {
        val retros =
            listOf(
                Retrospective.create(userId),
                Retrospective.create(userId),
            )
        whenever(retrospectiveFinder.searchByTitle(userId, "회고")).thenReturn(retros)

        val found = retrospectiveFinder.searchByTitle(userId, "회고")

        verify(retrospectiveFinder).searchByTitle(userId, "회고")
        assertThat(found).hasSize(2)
    }
}
