package com.didit.adapter.webapi.retrospect.dto

data class ContentResponse(
    val summary: String,
    val blockedPoint: List<String>,
    val solutionProcess: List<String>,
    val lessonLearned: List<String>,
    val insight: InsightContentResponse,
    val nextAction: NextActionContentResponse,
)

data class InsightContentResponse(
    val title: String,
    val description: String,
)

data class NextActionContentResponse(
    val title: String,
    val description: String,
)
