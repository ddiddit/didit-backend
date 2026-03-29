package com.didit.domain.auth

import com.didit.domain.shared.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Table(name = "withdrawal_records")
@Entity
class WithdrawalRecord(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val reason: WithdrawalReason,
    @Column(columnDefinition = "TEXT")
    val reasonDetail: String? = null,
) : BaseEntity() {
    companion object {
        fun create(
            userId: UUID,
            reason: WithdrawalReason,
            reasonDetail: String? = null,
        ): WithdrawalRecord {
            validateReasonDetail(reason, reasonDetail)
            return WithdrawalRecord(
                userId = userId,
                reason = reason,
                reasonDetail = reasonDetail,
            )
        }

        private fun validateReasonDetail(
            reason: WithdrawalReason,
            reasonDetail: String?,
        ) {
            if (reason == WithdrawalReason.OTHER) {
                require(!reasonDetail.isNullOrBlank()) { "기타 사유 선택 시 상세 내용을 입력해야 합니다." }
            }
        }
    }
}
