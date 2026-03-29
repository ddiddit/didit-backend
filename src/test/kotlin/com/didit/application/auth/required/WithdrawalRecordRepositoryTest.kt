package com.didit.application.auth.required

import com.didit.domain.auth.WithdrawalReason
import com.didit.domain.auth.WithdrawalRecord
import com.didit.support.RepositoryTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class WithdrawalRecordRepositoryTest : RepositoryTestSupport() {
    @Autowired
    lateinit var withdrawalRecordRepository: WithdrawalRecordRepository

    private val userId = UUID.randomUUID()

    @Test
    fun `save - 탈퇴 기록을 저장한다`() {
        val record =
            WithdrawalRecord.create(
                userId = userId,
                reason = WithdrawalReason.NO_LONGER_NEEDED,
            )

        val saved = withdrawalRecordRepository.save(record)

        assertThat(saved.userId).isEqualTo(userId)
        assertThat(saved.reason).isEqualTo(WithdrawalReason.NO_LONGER_NEEDED)
        assertThat(saved.reasonDetail).isNull()
    }

    @Test
    fun `save - 기타 사유로 탈퇴 기록을 저장한다`() {
        val record =
            WithdrawalRecord.create(
                userId = userId,
                reason = WithdrawalReason.OTHER,
                reasonDetail = "개인적인 사유입니다.",
            )

        val saved = withdrawalRecordRepository.save(record)

        assertThat(saved.reason).isEqualTo(WithdrawalReason.OTHER)
        assertThat(saved.reasonDetail).isEqualTo("개인적인 사유입니다.")
    }
}
