package com.didit.application.retrospect.dto

data class AISummaryResponse(
    val title: String,
    val feedback: String,
    val insight: String,
    val doneWork: String,
    val blockedPoint: String,
    val solutionProcess: String,
    val lessonLearned: String,
)
