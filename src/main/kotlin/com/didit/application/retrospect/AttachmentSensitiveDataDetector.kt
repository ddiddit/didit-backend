package com.didit.application.retrospect

import org.springframework.stereotype.Component

@Component
class AttachmentSensitiveDataDetector {
    fun containsSensitiveData(content: String): Boolean = sensitivePatterns.any { it.containsMatchIn(content) }

    fun redact(content: String): String = sensitivePatterns.fold(content) { redacted, pattern -> pattern.replace(redacted, MASK) }

    private companion object {
        const val MASK = "[민감정보]"
        val sensitivePatterns =
            listOf(
                Regex("""\b\d{6}[- ]?[1-4]\d{6}\b"""),
                Regex("""\b01[016789][- ]?\d{3,4}[- ]?\d{4}\b"""),
                Regex("""\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b""", RegexOption.IGNORE_CASE),
                Regex(
                    """\b(api[-_ ]?key|access[-_ ]?token|secret[-_ ]?key)\b\s*[:=]\s*[A-Z0-9_./+\-=]{12,}""",
                    RegexOption.IGNORE_CASE,
                ),
                Regex("""(비밀번호|password|passwd)\s*[:=]\s*\S{6,}""", RegexOption.IGNORE_CASE),
                Regex("""\bsk-[A-Z0-9_-]{20,}\b""", RegexOption.IGNORE_CASE),
            )
    }
}
