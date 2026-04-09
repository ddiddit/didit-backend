package com.didit.domain.retrospect

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class RetrospectiveSummary(
    @Column(columnDefinition = "TEXT")
    val summary: String,
    @Column(columnDefinition = "TEXT")
    val blockedPoint: String,
    @Column(columnDefinition = "TEXT")
    val solutionProcess: String,
    @Column(columnDefinition = "TEXT")
    val lessonLearned: String,
    @Column(columnDefinition = "TEXT")
    val insightTitle: String,
    @Column(columnDefinition = "TEXT")
    val insightDescription: String,
    @Column(columnDefinition = "TEXT")
    val nextActionTitle: String,
    @Column(columnDefinition = "TEXT")
    val nextActionDescription: String,
)
