package com.didit.application.achievement

import com.didit.application.achievement.provided.DailyAccessTracker
import com.didit.application.achievement.required.BadgeRepository
import com.didit.application.achievement.required.DailyAccessStreakRepository
import com.didit.application.achievement.required.OrganizationAchievementReader
import com.didit.application.achievement.required.RetrospectAchievementReader
import com.didit.application.achievement.required.UserBadgeRepository
import com.didit.application.achievement.required.WeeklyRetroStreakRepository
import com.didit.application.audit.AuditLogger
import com.didit.domain.achievement.Badge
import com.didit.domain.achievement.BadgeConditionType
import com.didit.domain.achievement.DailyAccessStreak
import com.didit.domain.achievement.UserBadge
import com.didit.support.BadgeFixture
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

    @Mock lateinit var weeklyRetroStreakRepository: WeeklyRetroStreakRepository

    @Mock lateinit var dailyAccessStreakRepository: DailyAccessStreakRepository

    @Mock lateinit var dailyAccessTracker: DailyAccessTracker

    @Mock lateinit var retrospectAchievementReader: RetrospectAchievementReader

    @Mock lateinit var organizationAchievementReader: OrganizationAchievementReader

    @Mock lateinit var auditLogger: AuditLogger

    private lateinit var badgeService: BadgeAwarderService

    private val userId = UUID.randomUUID()
    private val today = LocalDate.of(2026, 6, 24) // 수요일

    @BeforeEach
    fun setUp() {
        badgeService =
            BadgeAwarderService(
                badgeRepository = badgeRepository,
                userBadgeRepository = userBadgeRepository,
                weeklyRetroStreakRepository = weeklyRetroStreakRepository,
                dailyAccessStreakRepository = dailyAccessStreakRepository,
                dailyAccessTracker = dailyAccessTracker,
                retrospectAchievementReader = retrospectAchievementReader,
                organizationAchievementReader = organizationAchievementReader,
                badgeConditionChecker = BadgeConditionChecker(),
                auditLogger = auditLogger,
            )
    }

    private fun defaultMocks(
        totalRetroCount: Int = 1,
        currentWeekRetroCount: Int = 1,
        weeklyStreakWithMin3: Int = 0,
        projectCount: Int = 0,
        projectAssignedRetroCount: Int = 0,
        maxRetroInOneProject: Int = 0,
    ) {
        whenever(weeklyRetroStreakRepository.findByUserId(userId)).thenReturn(null)
        whenever(weeklyRetroStreakRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(dailyAccessStreakRepository.findByUserId(userId)).thenReturn(null)
        whenever(retrospectAchievementReader.countCompletedRetros(userId)).thenReturn(totalRetroCount)
        whenever(retrospectAchievementReader.countRetrosInWeek(eq(userId), any())).thenReturn(currentWeekRetroCount)
        whenever(retrospectAchievementReader.countConsecutiveWeeksWithMinRetros(userId, 3)).thenReturn(weeklyStreakWithMin3)
        whenever(organizationAchievementReader.countProjects(userId)).thenReturn(projectCount)
        whenever(organizationAchievementReader.countProjectAssignedRetros(userId)).thenReturn(projectAssignedRetroCount)
        whenever(organizationAchievementReader.maxRetroCountInOneProject(userId)).thenReturn(maxRetroInOneProject)
        whenever(userBadgeRepository.findAllByUserId(userId)).thenReturn(emptyList())
    }

    @Test
    fun `awardBadges - 첫 회고 시 CUMULATIVE_RETRO 1 배지를 부여한다`() {
        defaultMocks(totalRetroCount = 1)
        whenever(badgeRepository.findAll()).thenReturn(listOf(BadgeFixture.cumulativeRetro(1)))
        whenever(userBadgeRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = badgeService.awardBadges(userId, today)

        assertThat(result).hasSize(1)
        assertThat(result[0].conditionType).isEqualTo(BadgeConditionType.CUMULATIVE_RETRO)
        verify(userBadgeRepository).save(any())
    }

    @Test
    fun `awardBadges - 이미 획득한 배지는 다시 부여하지 않는다`() {
        val existingBadge = BadgeFixture.cumulativeRetro(1)
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
    fun `awardBadges - 비활성 배지는 후보에서 제외한다`() {
        val activeBadge = BadgeFixture.cumulativeRetro(1)
        val inactiveBadge: Badge = BadgeFixture.cumulativeRetro(1).apply { active = false }
        defaultMocks(totalRetroCount = 1)
        whenever(badgeRepository.findAll()).thenReturn(listOf(activeBadge, inactiveBadge))
        whenever(userBadgeRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = badgeService.awardBadges(userId, today)

        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo(activeBadge.id)
    }

    @Test
    fun `awardBadges - 주간 회고 스트릭을 갱신한다`() {
        defaultMocks(totalRetroCount = 1)
        whenever(badgeRepository.findAll()).thenReturn(emptyList())

        badgeService.awardBadges(userId, today)

        verify(weeklyRetroStreakRepository).save(any())
    }

    @Test
    fun `awardBadges - PROJECT_COUNT 조건 미충족 시 부여하지 않는다`() {
        defaultMocks(projectCount = 2)
        whenever(badgeRepository.findAll()).thenReturn(listOf(BadgeFixture.projectCount(3)))

        val result = badgeService.awardBadges(userId, today)

        assertThat(result).isEmpty()
        verify(userBadgeRepository, never()).save(any())
    }

    @Test
    fun `awardBadges - WEEKLY_STREAK weeklyMin 3 조건은 weeklyStreakWithMin3 값으로 판정된다`() {
        defaultMocks(weeklyStreakWithMin3 = 3)
        whenever(badgeRepository.findAll()).thenReturn(listOf(BadgeFixture.weeklyStreak(threshold = 3, weeklyMin = 3)))
        whenever(userBadgeRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = badgeService.awardBadges(userId, today)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `awardAccessBadges - 7일 연속 접속 시 디딧 러버 배지를 부여한다`() {
        val streak =
            DailyAccessStreak.create(userId).apply {
                repeat(7) { i -> recordAccess(today.minusDays((6 - i).toLong())) }
            }
        whenever(dailyAccessTracker.recordAccess(userId, today)).thenReturn(streak)
        whenever(userBadgeRepository.findAllByUserId(userId)).thenReturn(emptyList())
        whenever(badgeRepository.findAll()).thenReturn(listOf(BadgeFixture.dailyAccessStreak(7)))
        whenever(userBadgeRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = badgeService.awardAccessBadges(userId, today)

        assertThat(result).hasSize(1)
        assertThat(result[0].conditionType).isEqualTo(BadgeConditionType.DAILY_ACCESS_STREAK)
        verify(userBadgeRepository).save(any())
    }

    @Test
    fun `awardAccessBadges - 스트릭이 부족하면 부여하지 않는다`() {
        val streak =
            DailyAccessStreak.create(userId).apply {
                recordAccess(today)
            }
        whenever(dailyAccessTracker.recordAccess(userId, today)).thenReturn(streak)
        whenever(userBadgeRepository.findAllByUserId(userId)).thenReturn(emptyList())
        whenever(badgeRepository.findAll()).thenReturn(listOf(BadgeFixture.dailyAccessStreak(7)))

        val result = badgeService.awardAccessBadges(userId, today)

        assertThat(result).isEmpty()
        verify(userBadgeRepository, never()).save(any())
    }

    @Test
    fun `awardAccessBadges - 접속 외 조건 배지는 평가하지 않는다`() {
        val streak =
            DailyAccessStreak.create(userId).apply {
                repeat(7) { i -> recordAccess(today.minusDays((6 - i).toLong())) }
            }
        whenever(dailyAccessTracker.recordAccess(userId, today)).thenReturn(streak)
        whenever(userBadgeRepository.findAllByUserId(userId)).thenReturn(emptyList())
        whenever(badgeRepository.findAll()).thenReturn(
            listOf(
                BadgeFixture.dailyAccessStreak(7),
                BadgeFixture.cumulativeRetro(1),
            ),
        )
        whenever(userBadgeRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = badgeService.awardAccessBadges(userId, today)

        assertThat(result).hasSize(1)
        assertThat(result[0].conditionType).isEqualTo(BadgeConditionType.DAILY_ACCESS_STREAK)
    }

    private fun eq(value: UUID) = org.mockito.kotlin.eq(value)
}
