package com.didit.application.auth.required

import com.didit.domain.auth.WithdrawalRecord
import org.springframework.data.repository.Repository
import java.util.UUID

interface WithdrawalRecordRepository : Repository<WithdrawalRecord, UUID> {
    fun save(withdrawalRecord: WithdrawalRecord): WithdrawalRecord
}
