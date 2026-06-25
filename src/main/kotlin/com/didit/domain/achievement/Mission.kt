package com.didit.domain.achievement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "missions")
@Entity
class Mission(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    val level: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val missionType: MissionType,
    @Column(nullable = false)
    val targetCount: Int,
    @Column(nullable = false, length = 100)
    val title: String,
    @Column(nullable = false, length = 255)
    val description: String,
    @Column(length = 255)
    val subText: String? = null,
    @Column
    val durationDays: Int? = null,
    @Column(nullable = false)
    val isActive: Boolean = true,
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun firstRetro(): Mission =
            Mission(
                level = 1,
                missionType = MissionType.FIRST_RETRO,
                targetCount = 1,
                title = "첫 회고 작성하기",
                description = "첫 회고를 작성해보세요",
            )

        fun timeLimited(): Mission =
            Mission(
                level = 2,
                missionType = MissionType.TIME_LIMITED,
                targetCount = 3,
                title = "일주일 내에 회고 3회 작성하기",
                description = "7일 이내에 회고를 3회 이상 작성해보세요",
                durationDays = 7,
            )

        fun consecutiveWeek(
            level: Int,
            weeks: Int,
        ): Mission =
            Mission(
                level = level,
                missionType = MissionType.CONSECUTIVE_WEEK,
                targetCount = weeks,
                title = "${weeks}주 연속 주 1회 이상 회고",
                description = "매주 한 번씩 회고를 작성하면 달성할 수 있어요",
                subText = "꾸준한 기록이 습관을 만들어요",
            )

        fun cumulativeRetro(
            level: Int,
            count: Int,
        ): Mission =
            Mission(
                level = level,
                missionType = MissionType.CUMULATIVE_RETRO,
                targetCount = count,
                title = "회고 ${count}회 작성하기",
                description = "총 ${count}회의 회고를 작성해보세요",
                subText = "원하는 속도로 미션을 완료해보세요",
            )
    }
}
