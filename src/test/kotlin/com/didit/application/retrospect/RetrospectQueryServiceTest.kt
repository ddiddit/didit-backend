package com.didit.application.retrospect

import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.provided.SearchHistoryRegister
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.ChatMessage
import com.didit.domain.retrospect.QuestionType
import com.didit.domain.retrospect.RetroStatus
import com.didit.domain.retrospect.Retrospective
import com.didit.support.RetrospectiveFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RetrospectQueryServiceTest {
    @Mock lateinit var retrospectiveRepository: RetrospectiveRepository

    @Mock lateinit var searchHistoryRegister: SearchHistoryRegister

    private lateinit var retrospectQueryService: RetrospectQueryService

    private val userId = UUID.randomUUID()
    private val retrospectiveId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        retrospectQueryService =
            RetrospectQueryService(
                retrospectiveRepository = retrospectiveRepository,
                searchHistoryRegister = searchHistoryRegister,
            )
    }

    @Test
    fun `findById - 회고를 찾아 반환한다`() {
        val retro = Retrospective.create(userId)
        whenever(retrospectiveRepository.findByIdAndUserId(retrospectiveId, userId)).thenReturn(retro)

        val result = retrospectQueryService.findById(retrospectiveId, userId)

        assertThat(result.userId).isEqualTo(userId)
    }

    @Test
    fun `findById - 존재하지 않으면 예외가 발생한다`() {
        whenever(retrospectiveRepository.findByIdAndUserId(retrospectiveId, userId)).thenReturn(null)

        assertThrows<RetrospectiveNotFoundException> {
            retrospectQueryService.findById(retrospectiveId, userId)
        }
    }

    @Test
    fun `findAllByUserId - 완료된 회고만 반환한다`() {
        val retros = listOf(RetrospectiveFixture.createCompleted(userId))
        whenever(retrospectiveRepository.findAllCompletedByUserId(userId)).thenReturn(retros)

        val result = retrospectQueryService.findAllByUserId(userId)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `findRecentByUserId - limit만큼 최근 완료된 회고를 반환한다`() {
        val retros = listOf(RetrospectiveFixture.createCompleted(userId))
        whenever(
            retrospectiveRepository.findRecentCompletedByUserId(
                userId = userId,
                pageable = PageRequest.of(0, 1),
            ),
        ).thenReturn(retros)

        val result = retrospectQueryService.findRecentByUserId(userId, 1)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `countByUserIdAndDate - 오늘 회고 횟수를 반환한다`() {
        val today = LocalDate.now()
        whenever(
            retrospectiveRepository.countByUserIdAndStatusNotAndCreatedAtBetween(
                userId = userId,
                status = RetroStatus.PENDING,
                from = today.atStartOfDay(),
                to = today.atTime(23, 59, 59),
            ),
        ).thenReturn(2)

        val result = retrospectQueryService.countByUserIdAndDate(userId, today)

        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `findDeepQuestion - 심화 질문이 생성됐으면 isReady true를 반환한다`() {
        val retro =
            Retrospective.create(userId).apply {
                addMessage(ChatMessage.question(this, "심화 질문입니다.", QuestionType.Q4_DEEP))
            }
        whenever(retrospectiveRepository.findByIdAndUserId(retrospectiveId, userId)).thenReturn(retro)

        val result = retrospectQueryService.findDeepQuestion(retrospectiveId, userId)

        assertThat(result.isReady).isTrue()
        assertThat(result.content).isEqualTo("심화 질문입니다.")
    }

    @Test
    fun `findDeepQuestion - 심화 질문이 아직 없으면 isReady false를 반환한다`() {
        val retro = Retrospective.create(userId)
        whenever(retrospectiveRepository.findByIdAndUserId(retrospectiveId, userId)).thenReturn(retro)

        val result = retrospectQueryService.findDeepQuestion(retrospectiveId, userId)

        assertThat(result.isReady).isFalse()
        assertThat(result.content).isNull()
    }

    @Test
    fun `searchByTitle - 키워드로 회고를 검색하고 검색 기록을 저장한다`() {
        val keyword = "회고"
        val retros = listOf(Retrospective.create(userId).apply { updateTitle("오늘 회고") })
        whenever(retrospectiveRepository.searchByUserIdAndTitle(userId, keyword)).thenReturn(retros)

        val result = retrospectQueryService.searchByTitle(userId, keyword)

        assertThat(result).hasSize(1)
        verify(searchHistoryRegister).register(userId, keyword)
    }

    @Test
    fun `searchByTitle - 검색 결과가 없어도 검색 기록은 저장된다`() {
        val keyword = "없는키워드"
        whenever(retrospectiveRepository.searchByUserIdAndTitle(userId, keyword)).thenReturn(emptyList())

        val result = retrospectQueryService.searchByTitle(userId, keyword)

        assertThat(result).isEmpty()
        verify(searchHistoryRegister).register(userId, keyword)
    }
}
