package com.didit.application.retrospect

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore

@Component
class SummarySaveConcurrencyLimiter(
    @Value("\${retrospective.summary-save-max-concurrency:4}") maxConcurrency: Int,
) {
    private val semaphore =
        Semaphore(
            requireNotNull(maxConcurrency.takeIf { it > 0 }) {
                "retrospective.summary-save-max-concurrency must be greater than 0"
            },
            true,
        )

    fun <T> execute(action: () -> T): T {
        semaphore.acquire()
        try {
            return action()
        } finally {
            semaphore.release()
        }
    }
}
