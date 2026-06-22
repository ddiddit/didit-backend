package com.didit.domain.retrospect

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable

@Embeddable
class RetrospectiveSummary(
    @Column(columnDefinition = "TEXT")
    val summary: String,
    @Convert(converter = StringListJsonConverter::class)
    @Column(columnDefinition = "TEXT")
    val blockedPoint: List<String>,
    @Convert(converter = StringListJsonConverter::class)
    @Column(columnDefinition = "TEXT")
    val solutionProcess: List<String>,
    @Convert(converter = StringListJsonConverter::class)
    @Column(columnDefinition = "TEXT")
    val lessonLearned: List<String>,
    @Column(columnDefinition = "TEXT")
    val insightTitle: String,
    @Column(columnDefinition = "TEXT")
    val insightDescription: String,
    @Column(columnDefinition = "TEXT")
    val nextActionTitle: String,
    @Column(columnDefinition = "TEXT")
    val nextActionDescription: String,
)
