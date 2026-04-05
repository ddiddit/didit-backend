package com.didit.adapter.webapi.retrospect.dto

data class ContentResponse(
    val summary: String,
    val feedback: String,
    val insight: String,
    val doneWork: String,
    val blockedPoint: List<String>,
    val solutionProcess: List<String>,
    val lessonLearned: List<String>,
    val nextAction: List<String>,
)
