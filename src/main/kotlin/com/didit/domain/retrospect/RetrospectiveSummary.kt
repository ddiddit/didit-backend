package com.didit.domain.retrospect

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class RetrospectiveSummary(
    @Column(columnDefinition = "TEXT")
    val feedback: String,
    @Column(columnDefinition = "TEXT")
    val insight: String,
    @Column(columnDefinition = "TEXT")
    val doneWork: String,
    @Column(columnDefinition = "TEXT")
    val blockedPoint: String,
    @Column(columnDefinition = "TEXT")
    val solutionProcess: String,
    @Column(columnDefinition = "TEXT")
    val lessonLearned: String,
)
