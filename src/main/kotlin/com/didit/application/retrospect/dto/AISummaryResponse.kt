package com.didit.application.retrospect.dto

data class AISummaryResponse(
    val title: String,
    val summary: String,
    val blockedPoint: List<String>,
    val solutionProcess: List<String>,
    val lessonLearned: List<String>,
    val insight: InsightResponse,
    val nextAction: NextActionResponse,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
)

data class InsightResponse(
    val title: String,
    val description: String,
)

data class NextActionResponse(
    val title: String,
    val description: String,
)
