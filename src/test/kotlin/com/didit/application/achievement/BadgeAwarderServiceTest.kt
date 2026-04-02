package com.didit.application.achievement

import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.RetrospectAchievementReader
import com.didit.application.achievement.required.StreakRepository
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.BadgeConditionType
import com.didit.domain.achievement.Streak
import com.didit.domain.achievement.UserBadge
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class BadgeAwarderServiceTest {
    @Mock lateinit var badgeRepository: BadgeRepository

    @Mock lateinit var userBadgeRepository: UserBadgeRepository

    @Mock lateinit var streakRepository: StreakRepository

    @Mock lateinit var retrospectAchievementReader: RetrospectAchievementReader

    private lateinit var badgeService: BadgeAwarderService
    private lateinit var badgeConditionChecker: BadgeConditionChecker

    private val userId = UUID.randomUUID()
    private val today = LocalDate.now()

    @BeforeEach
    fun setUp() {
        badgeConditionChecker = BadgeConditionChecker()
        badgeService =
            BadgeAwarderService(
                badgeRepository = badgeRepository,
                userBadgeRepository = userBadgeRepository,
                streakRepository = streakRepository,
                retrospectAchievementReader = retrospectAchievementReader,
                badgeConditionChecker = badgeConditionChecker,
            )
    }

    private fun badge(conditionType: BadgeConditionType) =
        Badge.create(
            name = conditionType.name,
            description = "설명",
            conditionType = conditionType,
        )

    private fun defaultMocks(
        totalRetroCount: Int = 1,
        deepQuestionCount: Int = 0,
        weeklyGoalAchievedWeeks: Int = 0,
        existingStreak: Streak? = null,
    ) {
        whenever(streakRepository.findByUserId(userId)).thenReturn(existingStreak)
        whenever(streakRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(retrospectAchievementReader.countCompletedRetros(userId)).thenReturn(totalRetroCount)
        whenever(retrospectAchievementReader.countDeepQuestionAnswers(userId)).thenReturn(deepQuestionCount)
        whenever(retrospectAchievementReader.countWeeklyGoalAchievedWeeks(userId)).thenReturn(weeklyGoalAchievedWeeks)
        whenever(userBadgeRepository.findAllByUserId(userId)).thenReturn(emptyList())
    }

    @Test
    fun `awardBadges - 첫 회고 시 FIRST_RETRO 배지를 부여한다`() {
        defaultMocks(totalRetroCount = 1)
        whenever(badgeRepository.findAll()).thenReturn(listOf(badge(BadgeConditionType.FIRST_RETRO)))
        whenever(userBadgeRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = badgeService.awardBadges(userId, today)

        assertThat(result).hasSize(1)
        assertThat(result[0].conditionType).isEqualTo(BadgeConditionType.FIRST_RETRO)
        verify(userBadgeRepository).save(any())
    }

    @Test
    fun `awardBadges - 이미 획득한 배지는 다시 부여하지 않는다`() {
        val existingBadge = badge(BadgeConditionType.FIRST_RETRO)
        defaultMocks(totalRetroCount = 1)
        whenever(badgeRepository.findAll()).thenReturn(listOf(existingBadge))
        whenever(userBadgeRepository.findAllByUserId(userId)).thenReturn(
            listOf(UserBadge.create(userId, existingBadge.id)),
        )

        val result = badgeService.awardBadges(userId, today)

        assertThat(result).isEmpty()
        verify(userBadgeRepository, never()).save(any())
    }

    @Test
    fun `awardBadges - 3일 연속 회고 시 STREAK_3_DAYS 배지를 부여한다`() {
        val streak =
            Streak.create(userId).apply {
                update(today.minusDays(2))
                update(today.minusDays(1))
            }
        defaultMocks(existingStreak = streak)
        whenever(badgeRepository.findAll()).thenReturn(listOf(badge(BadgeConditionType.STREAK_3_DAYS)))
        whenever(userBadgeRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = badgeService.awardBadges(userId, today)

        assertThat(result).hasSize(1)
        assertThat(result[0].conditionType).isEqualTo(BadgeConditionType.STREAK_3_DAYS)
    }

    @Test
    fun `awardBadges - 조건 미충족 시 배지를 부여하지 않는다`() {
        defaultMocks(totalRetroCount = 1)
        whenever(badgeRepository.findAll()).thenReturn(listOf(badge(BadgeConditionType.TOTAL_30)))

        val result = badgeService.awardBadges(userId, today)

        assertThat(result).isEmpty()
        verify(userBadgeRepository, never()).save(any())
    }

    @Test
    fun `awardBadges - 스트릭이 없으면 새로 생성한다`() {
        whenever(streakRepository.findByUserId(userId)).thenReturn(null)
        whenever(streakRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(retrospectAchievementReader.countCompletedRetros(userId)).thenReturn(1)
        whenever(retrospectAchievementReader.countDeepQuestionAnswers(userId)).thenReturn(0)
        whenever(retrospectAchievementReader.countWeeklyGoalAchievedWeeks(userId)).thenReturn(0)
        whenever(userBadgeRepository.findAllByUserId(userId)).thenReturn(emptyList())
        whenever(badgeRepository.findAll()).thenReturn(emptyList())

        badgeService.awardBadges(userId, today)

        verify(streakRepository).save(any())
    }
}
