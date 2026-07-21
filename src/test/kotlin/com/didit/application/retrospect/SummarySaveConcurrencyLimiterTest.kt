package com.didit.application.retrospect

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SummarySaveConcurrencyLimiterTest {
    @Test
    fun `execute - simultaneously runs no more than configured number of actions`() {
        val limiter = SummarySaveConcurrencyLimiter(4)
        val executor = Executors.newFixedThreadPool(10)
        val ready = CountDownLatch(10)
        val start = CountDownLatch(1)
        val firstWaveEntered = CountDownLatch(4)
        val release = CountDownLatch(1)
        val active = AtomicInteger()
        val maxActive = AtomicInteger()
        val completed = AtomicInteger()

        try {
            val futures =
                (1..10).map {
                    executor.submit {
                        ready.countDown()
                        start.await()
                        limiter.execute {
                            val currentActive = active.incrementAndGet()
                            maxActive.accumulateAndGet(currentActive, ::maxOf)
                            firstWaveEntered.countDown()
                            try {
                                release.await()
                                completed.incrementAndGet()
                            } finally {
                                active.decrementAndGet()
                            }
                        }
                    }
                }

            assertThat(ready.await(1, TimeUnit.SECONDS)).isTrue()
            start.countDown()
            assertThat(firstWaveEntered.await(1, TimeUnit.SECONDS)).isTrue()
            assertThat(active.get()).isEqualTo(4)

            release.countDown()
            futures.forEach { it.get(1, TimeUnit.SECONDS) }

            assertThat(maxActive.get()).isEqualTo(4)
            assertThat(completed.get()).isEqualTo(10)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `execute - releases permit when action fails`() {
        val limiter = SummarySaveConcurrencyLimiter(1)

        assertThrows<IllegalStateException> {
            limiter.execute { throw IllegalStateException("failed") }
        }

        assertThat(limiter.execute { "completed" }).isEqualTo("completed")
    }

    @Test
    fun `constructor - rejects non-positive max concurrency`() {
        assertThrows<IllegalArgumentException> { SummarySaveConcurrencyLimiter(0) }
    }
}
