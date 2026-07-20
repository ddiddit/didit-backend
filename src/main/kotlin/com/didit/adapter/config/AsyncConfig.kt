package com.didit.adapter.config

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

@EnableAsync
@Configuration
class AsyncConfig {
    @Bean("taskExecutor")
    fun taskExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 4
            queueCapacity = 100
            setThreadNamePrefix("didit-async-")
        }

    @Bean("aiTaskExecutor")
    fun aiTaskExecutor(meterRegistry: MeterRegistry): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 4
            maxPoolSize = 8
            queueCapacity = 50
            setThreadNamePrefix("didit-ai-")
            setRejectedExecutionHandler(ThreadPoolExecutor.AbortPolicy())

            Gauge.builder("didit.ai.executor.active", this) { it.activeCount.toDouble() }.register(meterRegistry)
            Gauge
                .builder("didit.ai.executor.queue.size", this) {
                    it.threadPoolExecutor.queue.size
                        .toDouble()
                }.register(meterRegistry)
            Gauge
                .builder(
                    "didit.ai.executor.completed",
                    this,
                ) { it.threadPoolExecutor.completedTaskCount.toDouble() }
                .register(meterRegistry)
        }
}
