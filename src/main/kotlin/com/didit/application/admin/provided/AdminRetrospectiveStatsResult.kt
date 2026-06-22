package com.didit.application.admin.provided

data class AdminRetrospectiveStatsResult(
    val total: Long, // 전체 회고 수(진행 중 포함)
    val completed: Long, // 완료 회고 수
    val inProgress: Long, // 진행 중(PENDING + IN_PROGRESS)
    val completionRate: Double, // 완료율(%)
    val avgPerUser: Double, // 유저당 평균 완료 회고 수
    val textAnswerCount: Long, // 텍스트 답변 수
    val voiceAnswerCount: Long, // 음성 답변 수
    val dailyTrend: List<DailyRetroCount>, // 최근 30일 완료 추이
)
