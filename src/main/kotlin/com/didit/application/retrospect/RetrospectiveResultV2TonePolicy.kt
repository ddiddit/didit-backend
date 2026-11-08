package com.didit.application.retrospect

import com.didit.application.retrospect.required.GeneratedRetrospectiveResultV2
import com.didit.domain.retrospect.RetrospectiveResultDetail
import org.springframework.stereotype.Component

@Component
class RetrospectiveResultV2TonePolicy {
    fun normalize(generated: GeneratedRetrospectiveResultV2): GeneratedRetrospectiveResultV2 =
        generated.copy(
            summary = generated.summary?.toPoliteStyle(),
            strengths = generated.strengths?.map { it.toAnalysisNominalStyle() },
            improvements = generated.improvements?.map { it.toAnalysisNominalStyle() },
            processes = generated.processes?.map { it.toAnalysisNominalStyle() },
            learnings = generated.learnings?.map { it.toAnalysisNominalStyle() },
            insight = generated.insight?.toPoliteStyle(),
            nextActions = generated.nextActions?.map { it.toPoliteStyle() },
        )

    fun requiresRegeneration(generated: GeneratedRetrospectiveResultV2): Boolean =
        generated.userFacingSentences().any { narrative ->
            REPORT_STYLE_REMAINDER.containsMatchIn(narrative) || narrative.endsWithNominalMieum()
        } ||
            generated.analysisItems().any(ANALYSIS_SENTENCE_ENDING::containsMatchIn)

    private fun RetrospectiveResultDetail.toPoliteStyle(): RetrospectiveResultDetail = copy(description = description.toPoliteStyle())

    private fun String.toPoliteStyle(): String =
        REPORT_STYLE_ENDINGS
            .fold(this) { normalized, (ending, replacement) -> normalized.replace(ending, replacement) }
            .normalizePastNominalEnding()
            .normalizePastPlainEnding()

    private fun String.toAnalysisNominalStyle(): String {
        val original = trim()
        val normalized =
            ANALYSIS_NOMINAL_ENDINGS.fold(original) { normalized, (ending, replacement) ->
                normalized.replace(ending, replacement)
            }
        if (!ANALYSIS_SENTENCE_ENDING.containsMatchIn(normalized)) return normalized

        val content = original.trimEnd().trimEnd('.', '!', '?', '…').trimEnd()
        return "‘$content’라고 정리한 내용"
    }

    private fun String.normalizePastNominalEnding(): String =
        PAST_NOMINAL_ENDING.replace(this) { match ->
            val syllable = match.groupValues[1].single()
            if (syllable.hasFinalSsangSiot()) "${syllable}어요" else match.value
        }

    private fun String.normalizePastPlainEnding(): String =
        PAST_PLAIN_ENDING.replace(this) { match ->
            val syllable = match.groupValues[1].single()
            if (syllable.hasFinalSsangSiot()) "${syllable}어요" else match.value
        }

    private fun Char.hasFinalSsangSiot(): Boolean =
        code in HANGUL_SYLLABLE_START..HANGUL_SYLLABLE_END &&
            (code - HANGUL_SYLLABLE_START) % HANGUL_FINAL_COUNT == SSANG_SIOT_FINAL_INDEX

    private fun String.endsWithNominalMieum(): Boolean =
        trimEnd()
            .trimEnd('.', '!', '?', '…')
            .trimEnd()
            .lastOrNull()
            ?.hasFinalMieum() == true

    private fun Char.hasFinalMieum(): Boolean =
        code in HANGUL_SYLLABLE_START..HANGUL_SYLLABLE_END &&
            (code - HANGUL_SYLLABLE_START) % HANGUL_FINAL_COUNT == MIEUM_FINAL_INDEX

    private fun GeneratedRetrospectiveResultV2.userFacingSentences(): List<String> =
        buildList {
            summary?.let(::add)
            insight?.description?.let(::add)
            nextActions?.mapTo(this) { it.description }
        }

    private fun GeneratedRetrospectiveResultV2.analysisItems(): List<String> =
        buildList {
            strengths?.let(::addAll)
            improvements?.let(::addAll)
            processes?.let(::addAll)
            learnings?.let(::addAll)
        }

    companion object {
        private val SENTENCE_END = "(?=\\s*[.!?…]?(?:\\s|$))"
        private val PAST_NOMINAL_ENDING = Regex("([가-힣])음$SENTENCE_END")
        private val PAST_PLAIN_ENDING = Regex("([가-힣])다$SENTENCE_END")
        private val REPORT_STYLE_REMAINDER = Regex("(?:다|하였음|했음|함)$SENTENCE_END")
        private val ANALYSIS_SENTENCE_ENDING =
            Regex(
                "(?:해요|했어요|이에요|예요|돼요|있어요|없어요|아요|어요|여요|줘요|봐요|워요|라요|눠요|" +
                    "할게요|입니다|습니다|다)\\s*[.!?…]?\\s*$",
            )
        private const val HANGUL_SYLLABLE_START = 0xAC00
        private const val HANGUL_SYLLABLE_END = 0xD7A3
        private const val HANGUL_FINAL_COUNT = 28
        private const val MIEUM_FINAL_INDEX = 16
        private const val SSANG_SIOT_FINAL_INDEX = 20
        private val REPORT_STYLE_ENDINGS =
            listOf(
                Regex("하였음$SENTENCE_END") to "했어요",
                Regex("하였다$SENTENCE_END") to "했어요",
                Regex("했음$SENTENCE_END") to "했어요",
                Regex("했다$SENTENCE_END") to "했어요",
                Regex("함$SENTENCE_END") to "했어요",
                Regex("([가-힣]+)(았|었|였|웠|됐|했)다$SENTENCE_END") to "\$1\$2어요",
                Regex("([가-힣]+)인다$SENTENCE_END") to "\$1여요",
                Regex("([가-힣]+)한다$SENTENCE_END") to "\$1해요",
                Regex("([가-힣]+)된다$SENTENCE_END") to "\$1돼요",
                Regex("([가-힣]+)하다$SENTENCE_END") to "\$1해요",
                Regex("있다$SENTENCE_END") to "있어요",
                Regex("없다$SENTENCE_END") to "없어요",
                Regex("이다$SENTENCE_END") to "이에요",
            )
        private val ANALYSIS_NOMINAL_ENDINGS =
            listOf(
                Regex("(?:했습니다|했어요)\\s*[.!?…]?\\s*$") to "함",
                Regex("(?:합니다|해요)\\s*[.!?…]?\\s*$") to "함",
                Regex("(?:됩니다|돼요)\\s*[.!?…]?\\s*$") to "됨",
                Regex("(?:있습니다|있어요)\\s*[.!?…]?\\s*$") to "있음",
                Regex("(?:없습니다|없어요)\\s*[.!?…]?\\s*$") to "없음",
                Regex("(?:입니다|이에요|예요)\\s*[.!?…]?\\s*$") to "임",
                Regex("알아요\\s*[.!?…]?\\s*$") to "앎",
                Regex("나눠요\\s*[.!?…]?\\s*$") to "나눔",
                Regex("빨라요\\s*[.!?…]?\\s*$") to "빠름",
                Regex("할게요\\s*[.!?…]?\\s*$") to "할 계획",
                Regex("받습니다\\s*[.!?…]?\\s*$") to "받음",
                Regex("쌓는다\\s*[.!?…]?\\s*$") to "쌓음",
                Regex("았습니다\\s*[.!?…]?\\s*$") to "았음",
                Regex("었습니다\\s*[.!?…]?\\s*$") to "었음",
                Regex("였습니다\\s*[.!?…]?\\s*$") to "였음",
                Regex("(?:하였다|했다|한다|하다)\\s*[.!?…]?\\s*$") to "함",
                Regex("된다\\s*[.!?…]?\\s*$") to "됨",
                Regex("있다\\s*[.!?…]?\\s*$") to "있음",
                Regex("없다\\s*[.!?…]?\\s*$") to "없음",
                Regex("이다\\s*[.!?…]?\\s*$") to "임",
            )
    }
}
