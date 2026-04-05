package com.aptox.app.usage

/** 일별 카테고리 합산 사용량(ms). Firestore categoryStats 백업과 동일 스키마. */
data class DailyCategoryStatEntity(
    val date: String, // yyyyMMdd
    val category: String,
    val usageMs: Long,
)
