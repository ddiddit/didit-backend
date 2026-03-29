package com.didit.application.retrospect.provided

import com.didit.application.retrospect.dto.DeepQuestionResponse
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
    fun `findByUserIdAndYearMonth - 월별 회고 목록을 반환한다`() {
        val retros = listOf(Retrospective.create(userId), Retrospective.create(userId))
        whenever(retrospectiveFinder.findByUserIdAndYearMonth(userId, 2026, 3)).thenReturn(retros)

        val found = retrospectiveFinder.findByUserIdAndYearMonth(userId, 2026, 3)

        verify(retrospectiveFinder).findByUserIdAndYearMonth(userId, 2026, 3)
        assertThat(found).hasSize(2)
    }

    @Test
    fun `findByUserIdAndDate - 날짜별 회고 목록을 반환한다`() {
        val retros = listOf(Retrospective.create(userId))
        val date = LocalDate.of(2026, 3, 10)
        whenever(retrospectiveFinder.findByUserIdAndDate(userId, date)).thenReturn(retros)

        val found = retrospectiveFinder.findByUserIdAndDate(userId, date)

        verify(retrospectiveFinder).findByUserIdAndDate(userId, date)
        assertThat(found).hasSize(1)
    }

    @Test
    fun `findDeepQuestion - 심화 질문이 생성된 경우 isReady true를 반환한다`() {
        val response =
            DeepQuestionResponse(
                isReady = true,
                content = "비슷한 상황이 또 생긴다면 어떻게 하실 것 같나요?",
            )
        whenever(retrospectiveFinder.findDeepQuestion(retrospectiveId, userId)).thenReturn(response)

        val found = retrospectiveFinder.findDeepQuestion(retrospectiveId, userId)

        verify(retrospectiveFinder).findDeepQuestion(retrospectiveId, userId)
        assertThat(found.isReady).isTrue()
        assertThat(found.content).isNotBlank()
    }

    @Test
    fun `findDeepQuestion - 심화 질문이 아직 생성 중이면 isReady false를 반환한다`() {
        val response = DeepQuestionResponse(isReady = false)
        whenever(retrospectiveFinder.findDeepQuestion(retrospectiveId, userId)).thenReturn(response)

        val found = retrospectiveFinder.findDeepQuestion(retrospectiveId, userId)

        verify(retrospectiveFinder).findDeepQuestion(retrospectiveId, userId)
        assertThat(found.isReady).isFalse()
        assertThat(found.content).isNull()
    }
}
