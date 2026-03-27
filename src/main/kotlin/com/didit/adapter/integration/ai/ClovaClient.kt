package com.didit.adapter.integration.ai

import com.didit.adapter.integration.ai.prompt.ClovaPrompts
import com.didit.application.retrospect.required.AIClient
import com.didit.domain.auth.Job
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class ClovaClient(
    private val webClient: WebClient,
    @Value("\${clova.api.api-key}") private val apiKey: String,
    @Value("\${clova.api.api-gw-key}") private val apiGwKey: String,
    @Value("\${clova.api.request-id}") private val requestId: String,
) : AIClient {
    companion object {
        private const val CLOVA_API_URL = "https://clovastudio.apigw.ntruss.com/testapp/v1/completions/LK-D"
        private const val ALGORITHM = "HmacSHA256"
    }

    override fun generateDeepQuestion(
        job: Job,
        answers: List<String>,
    ): String {
        val prompt = ClovaPrompts.buildDeepQuestionPrompt(job, answers)
        return callClova(prompt)
    }

    override fun generateSummary(
        job: Job,
        allAnswers: List<String>,
    ): String {
        val prompt = ClovaPrompts.buildSummaryPrompt(job, allAnswers)
        return callClova(prompt)
    }

    private fun callClova(prompt: String): String {
        val timestamp = Instant.now().epochSecond.toString()
        val signature = generateSignature(timestamp)

        val request =
            ClovaRequest(
                messages =
                    listOf(
                        ClovaMessage(role = "system", content = "당신은 전문 회고 코치입니다."),
                        ClovaMessage(role = "user", content = prompt),
                    ),
                maxTokens = 500,
                temperature = 0.7,
                topP = 0.8,
                repeatPenalty = 5.0,
            )

        return webClient
            .post()
            .uri(CLOVA_API_URL)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("X-NCP-CLOVASTUDIO-API-KEY", apiKey)
            .header("X-NCP-APIGW-API-KEY", apiGwKey)
            .header("X-NCP-CLOVASTUDIO-REQUEST-ID", requestId)
            .header("X-NCP-CLOVASTUDIO-TIMESTAMP", timestamp)
            .header("X-NCP-CLOVASTUDIO-SIGNATURE", signature)
            .bodyValue(request)
            .retrieve()
            .bodyToMono<ClovaResponse>()
            .map { it.result.output.text }
            .block()
            ?: throw RuntimeException("Clova 응답을 받지 못했습니다.")
    }

    private fun generateSignature(timestamp: String): String {
        val secretKey = SecretKeySpec(apiGwKey.toByteArray(), ALGORITHM)
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(secretKey)

        val message = "$timestamp.$requestId"
        return Base64.getEncoder().encodeToString(mac.doFinal(message.toByteArray()))
    }
}

data class ClovaRequest(
    val messages: List<ClovaMessage>,
    val maxTokens: Int,
    val temperature: Double,
    val topP: Double,
    val repeatPenalty: Double,
)

data class ClovaMessage(
    val role: String,
    val content: String,
)

data class ClovaResponse(
    val result: ClovaResult,
)

data class ClovaResult(
    val output: ClovaOutput,
)

data class ClovaOutput(
    val text: String,
)
