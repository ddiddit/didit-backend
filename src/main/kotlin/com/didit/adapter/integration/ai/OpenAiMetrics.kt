package com.didit.adapter.integration.ai

import com.fasterxml.jackson.core.JsonProcessingException
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import java.net.SocketTimeoutException
import java.time.Duration

@Component
class OpenAiMetrics(
    private val meterRegistry: MeterRegistry,
) {
    fun <T> record(
        operation: String,
        block: () -> T,
    ): T {
        val sample = Timer.start(meterRegistry)
        try {
            return block().also { stop(sample, operation, "success") }
        } catch (exception: Exception) {
            val errorType = classify(exception)
            stop(sample, operation, if (errorType == "timeout") "timeout" else "error")
            meterRegistry
                .counter("didit.openai.request.errors", "operation", operation, "type", errorType)
                .increment()
            throw exception
        }
    }

    fun recordPromptCharacters(
        operation: String,
        count: Int,
    ) {
        DistributionSummary
            .builder("didit.openai.prompt.characters")
            .tag("operation", operation)
            .register(meterRegistry)
            .record(count.toDouble())
    }

    fun recordTokens(
        operation: String,
        inputTokens: Int,
        outputTokens: Int,
    ) {
        meterRegistry.counter("didit.openai.tokens", "operation", operation, "type", "input").increment(inputTokens.toDouble())
        meterRegistry.counter("didit.openai.tokens", "operation", operation, "type", "output").increment(outputTokens.toDouble())
    }

    private fun stop(
        sample: Timer.Sample,
        operation: String,
        status: String,
    ) {
        sample.stop(
            Timer
                .builder("didit.openai.request.duration")
                .tags("operation", operation, "status", status)
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
                ).register(meterRegistry),
        )
    }

    private fun classify(exception: Exception): String =
        when (exception) {
            is RestClientResponseException ->
                when {
                    exception.statusCode.value() == 429 -> "rate_limit"
                    exception.statusCode.is4xxClientError -> "client_error"
                    exception.statusCode.is5xxServerError -> "server_error"
                    else -> "http_error"
                }

            is ResourceAccessException ->
                if (exception.causeSequence().any { it is SocketTimeoutException }) "timeout" else "connection_error"

            is JsonProcessingException -> "parse_error"
            else -> "unknown"
        }

    private fun Throwable.causeSequence(): Sequence<Throwable> = generateSequence(this) { it.cause }
}
