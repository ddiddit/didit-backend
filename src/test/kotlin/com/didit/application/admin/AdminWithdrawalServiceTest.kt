package com.didit.application.admin

import com.didit.application.auth.required.WithdrawalRecordRepository
import com.didit.domain.auth.WithdrawalReason
import com.didit.domain.auth.WithdrawalRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminWithdrawalServiceTest {
    @Mock
    lateinit var withdrawalRecordRepository: WithdrawalRecordRepository

    @InjectMocks
    lateinit var adminWithdrawalService: AdminWithdrawalService

    private fun createRecord(reason: WithdrawalReason) =
        WithdrawalRecord(
            userId = UUID.randomUUID(),
            reason = reason,
        )

    @Test
    fun `탈퇴 통계 조회 - 건수와 비율이 계산된다`() {
        val records =
            listOf(
                createRecord(WithdrawalReason.NO_LONGER_NEEDED),
                createRecord(WithdrawalReason.NO_LONGER_NEEDED),
                createRecord(WithdrawalReason.MISSING_FEATURES),
            )
        whenever(withdrawalRecordRepository.findAll()).thenReturn(records)

        val result = adminWithdrawalService.getWithdrawalStats()

        assertThat(result.total).isEqualTo(3)
        val noLongerNeeded = result.breakdown.find { it.reason == "NO_LONGER_NEEDED" }
        assertThat(noLongerNeeded?.count).isEqualTo(2)
        assertThat(noLongerNeeded?.percentage).isCloseTo(66.67, within(0.01))
    }

    @Test
    fun `탈퇴 통계 조회 - 건수 내림차순 정렬`() {
        val records =
            listOf(
                createRecord(WithdrawalReason.MISSING_FEATURES),
                createRecord(WithdrawalReason.NO_LONGER_NEEDED),
                createRecord(WithdrawalReason.NO_LONGER_NEEDED),
            )
        whenever(withdrawalRecordRepository.findAll()).thenReturn(records)

        val result = adminWithdrawalService.getWithdrawalStats()

        assertThat(result.breakdown[0].reason).isEqualTo("NO_LONGER_NEEDED")
        assertThat(result.breakdown[0].count).isEqualTo(2)
    }

    @Test
    fun `탈퇴 통계 조회 - 기록이 없으면 비율이 0이다`() {
        whenever(withdrawalRecordRepository.findAll()).thenReturn(emptyList())

        val result = adminWithdrawalService.getWithdrawalStats()

        assertThat(result.total).isEqualTo(0)
        assertThat(result.breakdown).isEmpty()
    }
}
