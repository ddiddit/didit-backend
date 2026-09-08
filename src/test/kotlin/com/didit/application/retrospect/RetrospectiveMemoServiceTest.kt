package com.didit.application.retrospect

import com.didit.application.retrospect.exception.DuplicateRetrospectiveMemoException
import com.didit.application.retrospect.exception.InvalidRetrospectiveMemoContentException
import com.didit.application.retrospect.exception.RetrospectiveNotFoundException
import com.didit.application.retrospect.required.RetrospectiveMemoRepository
import com.didit.application.retrospect.required.RetrospectiveRepository
import com.didit.domain.retrospect.Retrospective
import com.didit.domain.retrospect.RetrospectiveMemo
import com.didit.domain.retrospect.RetrospectiveSummary
import com.didit.domain.shared.ServiceTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class RetrospectiveMemoServiceTest {
    @Mock
    lateinit var retrospectiveRepository: RetrospectiveRepository

    @Mock
    lateinit var retrospectiveMemoRepository: RetrospectiveMemoRepository

    @InjectMocks
    lateinit var retrospectiveMemoService: RetrospectiveMemoService

    private val userId = UUID.randomUUID()
    private val retrospectiveId = UUID.randomUUID()

    @Test
    fun `완료된 회고에 오늘의 메모를 작성한다`() {
        val retrospective = completedRetrospective()
        val content = "다음 배포 전에 예외 케이스를 점검한다."

        whenever(retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNullForUpdate(retrospectiveId, userId)).thenReturn(retrospective)
        whenever(retrospectiveMemoRepository.existsByRetrospectiveIdAndMemoDate(retrospectiveId, ServiceTime.today())).thenReturn(false)
        whenever(retrospectiveMemoRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectiveMemoService.create(retrospectiveId, userId, content)

        assertThat(result.content).isEqualTo(content)
        assertThat(result.memoDate).isEqualTo(ServiceTime.today())
    }

    @Test
    fun `메모 작성 중에는 회고를 잠가 같은 날짜의 중복 작성을 직렬화한다`() {
        val retrospective = completedRetrospective()

        whenever(retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNullForUpdate(retrospectiveId, userId)).thenReturn(retrospective)
        whenever(retrospectiveMemoRepository.existsByRetrospectiveIdAndMemoDate(retrospectiveId, ServiceTime.today())).thenReturn(false)
        whenever(retrospectiveMemoRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = retrospectiveMemoService.create(retrospectiveId, userId, "동시 작성 방지 메모")

        assertThat(result.content).isEqualTo("동시 작성 방지 메모")
    }

    @Test
    fun `같은 회고에 같은 날짜 메모가 있으면 추가 작성할 수 없다`() {
        whenever(
            retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNullForUpdate(retrospectiveId, userId),
        ).thenReturn(completedRetrospective())
        whenever(retrospectiveMemoRepository.existsByRetrospectiveIdAndMemoDate(retrospectiveId, ServiceTime.today())).thenReturn(true)

        assertThrows<DuplicateRetrospectiveMemoException> {
            retrospectiveMemoService.create(retrospectiveId, userId, "이미 작성한 메모")
        }
    }

    @Test
    fun `타인의 회고에는 메모를 작성할 수 없다`() {
        whenever(retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNullForUpdate(retrospectiveId, userId)).thenReturn(null)

        assertThrows<RetrospectiveNotFoundException> {
            retrospectiveMemoService.create(retrospectiveId, userId, "권한 없는 메모")
        }
    }

    @Test
    fun `공백 메모는 작성할 수 없다`() {
        whenever(
            retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNullForUpdate(retrospectiveId, userId),
        ).thenReturn(completedRetrospective())
        whenever(retrospectiveMemoRepository.existsByRetrospectiveIdAndMemoDate(retrospectiveId, ServiceTime.today())).thenReturn(false)

        assertThrows<InvalidRetrospectiveMemoContentException> {
            retrospectiveMemoService.create(retrospectiveId, userId, "   ")
        }
    }

    @Test
    fun `메모 목록은 최신 메모 날짜 순서로 조회한다`() {
        val older = RetrospectiveMemo(retrospectiveId = retrospectiveId, content = "어제 메모", memoDate = LocalDate.of(2026, 9, 7))
        val newer = RetrospectiveMemo(retrospectiveId = retrospectiveId, content = "오늘 메모", memoDate = LocalDate.of(2026, 9, 8))

        whenever(retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNull(retrospectiveId, userId)).thenReturn(completedRetrospective())
        whenever(retrospectiveMemoRepository.findAllByRetrospectiveIdOrderByMemoDateDesc(retrospectiveId)).thenReturn(listOf(newer, older))

        val result = retrospectiveMemoService.findAll(retrospectiveId, userId)

        assertThat(result.map { it.content }).containsExactly("오늘 메모", "어제 메모")
    }

    @Test
    fun `본인 회고의 메모만 수정할 수 있다`() {
        val memo = RetrospectiveMemo(retrospectiveId = retrospectiveId, content = "수정 전", memoDate = ServiceTime.today())
        val updated = "수정 후 메모"

        whenever(retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNull(retrospectiveId, userId)).thenReturn(completedRetrospective())
        whenever(retrospectiveMemoRepository.findByIdAndRetrospectiveId(memo.id, retrospectiveId)).thenReturn(memo)

        val result = retrospectiveMemoService.update(retrospectiveId, memo.id, userId, updated)

        assertThat(result.content).isEqualTo(updated)
    }

    @Test
    fun `본인 회고의 메모만 삭제할 수 있다`() {
        val memo = RetrospectiveMemo(retrospectiveId = retrospectiveId, content = "삭제할 메모", memoDate = ServiceTime.today())

        whenever(retrospectiveRepository.findByIdAndUserIdAndDeletedAtIsNull(retrospectiveId, userId)).thenReturn(completedRetrospective())
        whenever(retrospectiveMemoRepository.findByIdAndRetrospectiveId(memo.id, retrospectiveId)).thenReturn(memo)

        retrospectiveMemoService.delete(retrospectiveId, memo.id, userId)

        org.mockito.kotlin
            .verify(retrospectiveMemoRepository)
            .delete(memo)
    }

    private fun completedRetrospective(): Retrospective =
        Retrospective(id = retrospectiveId, userId = userId).apply {
            saveSummary(
                RetrospectiveSummary(
                    summary = "회고 요약",
                    blockedPoint = emptyList(),
                    solutionProcess = emptyList(),
                    lessonLearned = emptyList(),
                    insightTitle = "인사이트",
                    insightDescription = "인사이트 설명",
                    nextActionTitle = "다음 행동",
                    nextActionDescription = "다음 행동 설명",
                ),
            )
            complete("메모 테스트 회고")
        }
}
