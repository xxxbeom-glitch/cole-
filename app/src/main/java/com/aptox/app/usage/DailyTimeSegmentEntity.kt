package com.aptox.app.usage

/** 일별 앱 2시간×12슬롯 사용량(ms). Firestore timeSegments 백업과 동일. */
data class DailyTimeSegmentEntity(
    val date: String, // yyyyMMdd
    val packageName: String,
    val slotMs: LongArray,
) {
    init {
        require(slotMs.size == TIME_SEGMENT_SLOT_COUNT)
    }

    companion object {
        const val TIME_SEGMENT_SLOT_COUNT = 12
    }
}
