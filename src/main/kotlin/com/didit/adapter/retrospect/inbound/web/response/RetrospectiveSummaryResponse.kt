package com.didit.adapter.retrospect.inbound.web.response

data class RetrospectiveSummaryResponse(
    val doneWork: String,
    val blockedPoint: String,
    val solutionProcess: String,
    val lessonLearned: String,
    val insight: String,
    val improvementDirection: String,
)
