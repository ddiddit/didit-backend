package com.didit.application.retrospect.dto

data class AISummaryResponse(
    val title: String,
    val summary: String,
    val feedback: String,
    val insight: String,
    val doneWork: String,
    val blockedPoint: List<String>,
    val solutionProcess: List<String>,
    val lessonLearned: List<String>,
    val nextAction: List<String>,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
)
