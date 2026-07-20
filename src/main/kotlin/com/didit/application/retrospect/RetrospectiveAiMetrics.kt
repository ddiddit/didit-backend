package com.didit.application.retrospect

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration

@Component
class RetrospectiveAiMetrics(
    private val meterRegistry: MeterRegistry,
) {
    private val transactionViolation = Counter.builder("didit.ai.transaction.active").register(meterRegistry)

    fun <T> recordStage(
        operation: String,
        stage: String,
        block: () -> T,
    ): T = workflowTimer(operation, stage).recordCallable(block)!!

    fun recordExternalCallTransactionState(operation: String) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            transactionViolation.increment()
        }
        meterRegistry
            .counter(
                "didit.ai.external.call",
                "operation",
                operation,
                "transaction_active",
                TransactionSynchronizationManager.isActualTransactionActive().toString(),
            ).increment()
    }

    fun incrementRejected() {
        meterRegistry.counter("didit.ai.executor.rejected").increment()
    }

    private fun workflowTimer(
        operation: String,
        stage: String,
    ): Timer =
        Timer
            .builder("didit.ai.workflow.duration")
            .tags("operation", operation, "stage", stage)
            .publishPercentileHistogram()
            .serviceLevelObjectives(
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(3),
                Duration.ofSeconds(4),
                Duration.ofSeconds(5),
                Duration.ofMillis(7500),
                Duration.ofSeconds(10),
                Duration.ofSeconds(15),
            ).register(meterRegistry)
}
