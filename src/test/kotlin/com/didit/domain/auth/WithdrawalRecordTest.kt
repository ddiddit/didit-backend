package com.didit.domain.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class WithdrawalRecordTest {
    private val userId = UUID.randomUUID()

    @Test
    fun `create - 정상적으로 탈퇴 기록을 생성한다`() {
        val record =
            WithdrawalRecord.create(
                userId = userId,
                reason = WithdrawalReason.NO_LONGER_NEEDED,
            )

        assertThat(record.userId).isEqualTo(userId)
        assertThat(record.reason).isEqualTo(WithdrawalReason.NO_LONGER_NEEDED)
        assertThat(record.reasonDetail).isNull()
    }

    @Test
    fun `create - OTHER 사유에 상세 내용을 입력하면 정상 생성된다`() {
        val record =
            WithdrawalRecord.create(
                userId = userId,
                reason = WithdrawalReason.OTHER,
                reasonDetail = "개인적인 사유입니다.",
            )

        assertThat(record.reason).isEqualTo(WithdrawalReason.OTHER)
        assertThat(record.reasonDetail).isEqualTo("개인적인 사유입니다.")
    }

    @Test
    fun `create - OTHER 사유에 상세 내용이 없으면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            WithdrawalRecord.create(
                userId = userId,
                reason = WithdrawalReason.OTHER,
                reasonDetail = null,
            )
        }
    }

    @Test
    fun `create - OTHER 사유에 상세 내용이 빈 값이면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            WithdrawalRecord.create(
                userId = userId,
                reason = WithdrawalReason.OTHER,
                reasonDetail = "",
            )
        }
    }

    @Test
    fun `create - OTHER가 아닌 사유에 상세 내용이 없어도 정상 생성된다`() {
        val record =
            WithdrawalRecord.create(
                userId = userId,
                reason = WithdrawalReason.MISSING_FEATURES,
                reasonDetail = null,
            )

        assertThat(record.reason).isEqualTo(WithdrawalReason.MISSING_FEATURES)
        assertThat(record.reasonDetail).isNull()
    }
}
