package com.didit.application.admin

import com.didit.application.admin.provided.AdminWithdrawalStatsFinder
import com.didit.application.admin.provided.AdminWithdrawalStatsResult
import com.didit.application.admin.provided.WithdrawalReasonCount
import com.didit.application.auth.required.WithdrawalRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class AdminWithdrawalService(
    private val withdrawalRecordRepository: WithdrawalRecordRepository,
) : AdminWithdrawalStatsFinder {
    override fun getWithdrawalStats(): AdminWithdrawalStatsResult {
        val records = withdrawalRecordRepository.findAll()
        val total = records.size.toLong()

        val breakdown =
            records
                .groupBy { it.reason.name }
                .map { (reason, list) ->
                    WithdrawalReasonCount(
                        reason = reason,
                        count = list.size.toLong(),
                        percentage = if (total == 0L) 0.0 else list.size.toDouble() / total * 100,
                    )
                }.sortedByDescending { it.count }

        return AdminWithdrawalStatsResult(total = total, breakdown = breakdown)
    }
}
